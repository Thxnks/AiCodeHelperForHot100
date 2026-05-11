package com.yupi.aicodehelper.agent.core;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkillCatalogService {

    private static final String SKILL_PATTERN = "classpath*:skills/*/SKILL.md";

    private final Map<String, String> fixedSkills;

    public SkillCatalogService() {
        this.fixedSkills = null;
    }

    private SkillCatalogService(Map<String, String> fixedSkills) {
        this.fixedSkills = new LinkedHashMap<>(fixedSkills);
    }

    public static SkillCatalogService of(Map<String, String> skills) {
        return new SkillCatalogService(skills);
    }

    public List<SkillMetadata> listSkills() {
        return loadSkills().entrySet().stream()
                .map(entry -> new SkillMetadata(entry.getKey(), extractDescription(entry.getValue())))
                .toList();
    }

    public SkillContent loadSkill(String name) {
        String content = loadSkills().get(name);
        if (content == null) {
            return new SkillContent(name, "Skill not found: " + name);
        }
        return new SkillContent(name, content);
    }

    private Map<String, String> loadSkills() {
        if (fixedSkills != null) {
            return fixedSkills;
        }
        Map<String, String> skills = new LinkedHashMap<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(SKILL_PATTERN);
            for (Resource resource : resources) {
                String path = resource.getURL().getPath().replace('\\', '/');
                String name = extractName(path);
                if (name == null || name.isBlank()) {
                    continue;
                }
                skills.put(name, resource.getContentAsString(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return skills;
    }

    private String extractName(String path) {
        int marker = path.lastIndexOf("/skills/");
        if (marker < 0) {
            return null;
        }
        String suffix = path.substring(marker + "/skills/".length());
        int slash = suffix.indexOf('/');
        return slash <= 0 ? null : suffix.substring(0, slash);
    }

    private String extractDescription(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String[] lines = content.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }
            return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 160);
        }
        return "";
    }
}
