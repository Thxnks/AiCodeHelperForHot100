package com.yupi.aicodehelper.agent;

import com.yupi.aicodehelper.entity.AgentMemory;
import com.yupi.aicodehelper.repository.AgentMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AgentMemoryService {

    private static final int MAX_IMPORTANCE = 10;
    private static final int MIN_IMPORTANCE = 1;

    private final AgentMemoryRepository agentMemoryRepository;

    public AgentMemoryService(AgentMemoryRepository agentMemoryRepository) {
        this.agentMemoryRepository = agentMemoryRepository;
    }

    @Transactional
    public AgentMemoryView remember(Long userId,
                                    String rawType,
                                    String scope,
                                    String subject,
                                    String content,
                                    int importance,
                                    String source) {
        return remember(userId, parseType(rawType), scope, subject, content, importance, source);
    }

    @Transactional
    public AgentMemoryView remember(Long userId,
                                    AgentMemoryType type,
                                    String scope,
                                    String subject,
                                    String content,
                                    int importance,
                                    String source) {
        AgentMemory memory = new AgentMemory();
        memory.setMemoryId(UUID.randomUUID().toString().replace("-", ""));
        memory.setUserId(userId);
        memory.setType(type.name());
        memory.setScope(defaultIfBlank(scope, "global"));
        memory.setSubject(requireText(subject, "subject"));
        memory.setContent(requireText(content, "content"));
        memory.setImportance(normalizeImportance(importance));
        memory.setSource(defaultIfBlank(source, "agent"));
        return AgentMemoryView.from(agentMemoryRepository.save(memory));
    }

    @Transactional(readOnly = true)
    public List<AgentMemoryView> recall(Long userId, String query, int limit) {
        int realLimit = Math.max(1, Math.min(limit, 10));
        List<String> terms = splitTerms(query);
        return agentMemoryRepository.findByUserIdOrderByImportanceDescUpdatedAtDesc(userId).stream()
                .map(memory -> new ScoredMemory(memory, score(memory, terms)))
                .filter(item -> terms.isEmpty() || item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredMemory::score).reversed()
                        .thenComparing(item -> item.memory().getImportance(), Comparator.reverseOrder())
                        .thenComparing(item -> item.memory().getUpdatedAt(), Comparator.reverseOrder()))
                .limit(realLimit)
                .map(item -> AgentMemoryView.from(item.memory()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentMemoryProfileView profile(Long userId) {
        List<AgentMemory> memories = agentMemoryRepository.findTop20ByUserIdOrderByImportanceDescUpdatedAtDesc(userId);
        return new AgentMemoryProfileView(
                contentsByType(memories, AgentMemoryType.WEAKNESS, 5),
                contentsByType(memories, AgentMemoryType.WRONG_ANSWER, 5),
                contentsByType(memories, AgentMemoryType.NEXT_ACTION, 5),
                contentsByType(memories, AgentMemoryType.USER_PREFERENCE, 5),
                memories.stream().limit(8).map(AgentMemoryView::from).toList()
        );
    }

    @Transactional
    public void rememberProgress(Long userId,
                                 String problemSlug,
                                 String status,
                                 String notes,
                                 String knowledgePoint,
                                 String wrongReason,
                                 String nextAction) {
        if (isBlank(problemSlug)) {
            return;
        }
        if (!isBlank(notes)) {
            remember(userId, AgentMemoryType.NOTE, "hot100", problemSlug,
                    "User note for " + problemSlug + ": " + notes, 4, "progress");
        }
        if (!isBlank(knowledgePoint)) {
            remember(userId, AgentMemoryType.WEAKNESS, "hot100", problemSlug,
                    "Needs reinforcement on " + knowledgePoint + " from " + problemSlug + ".", 7, "progress");
        }
        if (!isBlank(wrongReason)) {
            remember(userId, AgentMemoryType.WRONG_ANSWER, "hot100", problemSlug,
                    "Wrong-answer pattern on " + problemSlug + ": " + wrongReason, 8, "progress");
        }
        if (!isBlank(nextAction)) {
            remember(userId, AgentMemoryType.NEXT_ACTION, "hot100", problemSlug,
                    "Next action after " + problemSlug + ": " + nextAction, 6, "progress");
        }
        if ("MASTERED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            remember(userId, AgentMemoryType.NOTE, "hot100", problemSlug,
                    "User has completed " + problemSlug + " with status " + status + ".", 3, "progress");
        }
    }

    private List<String> contentsByType(List<AgentMemory> memories, AgentMemoryType type, int limit) {
        return memories.stream()
                .filter(memory -> type.name().equals(memory.getType()))
                .limit(limit)
                .map(AgentMemory::getContent)
                .toList();
    }

    private int score(AgentMemory memory, List<String> terms) {
        if (terms.isEmpty()) {
            return memory.getImportance();
        }
        String haystack = String.join(" ",
                memory.getType(),
                memory.getScope(),
                memory.getSubject(),
                memory.getContent(),
                memory.getSource()
        ).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            int index = haystack.indexOf(term);
            while (index >= 0) {
                score++;
                index = haystack.indexOf(term, index + term.length());
            }
        }
        return score + memory.getImportance();
    }

    private List<String> splitTerms(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ");
        List<String> terms = new ArrayList<>();
        for (String term : normalized.split("\\s+")) {
            if (term.length() >= 2) {
                terms.add(term);
            }
        }
        return terms.stream().distinct().toList();
    }

    private int normalizeImportance(int importance) {
        return Math.max(MIN_IMPORTANCE, Math.min(importance, MAX_IMPORTANCE));
    }

    private AgentMemoryType parseType(String value) {
        if (isBlank(value)) {
            return AgentMemoryType.NOTE;
        }
        try {
            return AgentMemoryType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AgentMemoryType.NOTE;
        }
    }

    private String requireText(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ScoredMemory(AgentMemory memory, int score) {
    }
}
