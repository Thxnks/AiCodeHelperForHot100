package com.yupi.aicodehelper.agent;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AgentKnowledgeService {

    private static final int MAX_CONTENT_LENGTH = 700;

    private final ObjectProvider<ContentRetriever> contentRetrieverProvider;

    private volatile List<LocalKnowledgeDocument> localDocuments;

    public AgentKnowledgeService(ObjectProvider<ContentRetriever> contentRetrieverProvider) {
        this.contentRetrieverProvider = contentRetrieverProvider;
    }

    public List<KnowledgeSnippetView> retrieve(String query, int limit) {
        int realLimit = Math.max(1, Math.min(limit, 8));
        List<KnowledgeSnippetView> vectorResults = retrieveWithVectorStore(query, realLimit);
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
        return retrieveWithKeywordSearch(query, realLimit);
    }

    private List<KnowledgeSnippetView> retrieveWithVectorStore(String query, int limit) {
        ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
        if (contentRetriever == null || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return contentRetriever.retrieve(Query.from(query)).stream()
                    .limit(limit)
                    .map(this::toSnippet)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private KnowledgeSnippetView toSnippet(Content content) {
        String source = "vector-store";
        if (content.textSegment() != null && content.textSegment().metadata() != null) {
            String fileName = content.textSegment().metadata().getString("file_name");
            if (fileName != null && !fileName.isBlank()) {
                source = fileName;
            }
        }
        String text = content.textSegment() == null ? "" : content.textSegment().text();
        return new KnowledgeSnippetView(source, truncate(text, MAX_CONTENT_LENGTH));
    }

    private List<KnowledgeSnippetView> retrieveWithKeywordSearch(String query, int limit) {
        List<String> terms = splitTerms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return localDocuments().stream()
                .map(document -> new ScoredDocument(document, score(document.content(), terms)))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
                .limit(limit)
                .map(item -> new KnowledgeSnippetView(item.document().source(), truncate(bestWindow(item.document().content(), terms), MAX_CONTENT_LENGTH)))
                .toList();
    }

    private List<LocalKnowledgeDocument> localDocuments() {
        List<LocalKnowledgeDocument> snapshot = localDocuments;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (localDocuments == null) {
                localDocuments = loadLocalDocuments();
            }
            return localDocuments;
        }
    }

    private List<LocalKnowledgeDocument> loadLocalDocuments() {
        List<LocalKnowledgeDocument> documents = new ArrayList<>();
        documents.addAll(loadResources("classpath:docs/**/*.md"));
        documents.addAll(loadResources("classpath:hot100/markdown/*.md"));
        return documents;
    }

    private List<LocalKnowledgeDocument> loadResources(String pattern) {
        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(null);
            Resource[] resources = resolver.getResources(pattern);
            List<LocalKnowledgeDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                if (!content.isBlank()) {
                    documents.add(new LocalKnowledgeDocument(filename, content));
                }
            }
            return documents;
        } catch (IOException e) {
            return List.of();
        }
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
        return terms;
    }

    private int score(String content, List<String> terms) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            int index = normalized.indexOf(term);
            while (index >= 0) {
                score++;
                index = normalized.indexOf(term, index + term.length());
            }
        }
        return score;
    }

    private String bestWindow(String content, List<String> terms) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int bestIndex = 0;
        for (String term : terms) {
            int index = normalized.indexOf(term);
            if (index >= 0) {
                bestIndex = index;
                break;
            }
        }
        int start = Math.max(0, bestIndex - 120);
        int end = Math.min(content.length(), bestIndex + MAX_CONTENT_LENGTH);
        return content.substring(start, end).trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record LocalKnowledgeDocument(String source, String content) {
    }

    private record ScoredDocument(LocalKnowledgeDocument document, int score) {
    }
}
