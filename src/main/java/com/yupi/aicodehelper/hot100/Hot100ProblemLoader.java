package com.yupi.aicodehelper.hot100;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.config.properties.AppHot100Properties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class Hot100ProblemLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    private final ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper;

    private final AppHot100Properties appHot100Properties;

    private final Map<String, Hot100Problem> problemMap = new LinkedHashMap<>();

    public Hot100ProblemLoader(ResourcePatternResolver resourcePatternResolver,
                               ResourceLoader resourceLoader,
                               ObjectMapper objectMapper,
                               AppHot100Properties appHot100Properties) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.appHot100Properties = appHot100Properties;
    }

    @PostConstruct
    public void init() {
        load();
    }

    public List<Hot100Problem> listAll() {
        return new ArrayList<>(problemMap.values());
    }

    public Hot100Problem getBySlug(String slug) {
        return problemMap.get(slug);
    }

    private void load() {
        String metadataPath = appHot100Properties.getMetadataPath();
        String markdownBasePath = appHot100Properties.getMarkdownBasePath();
        try {
            Resource[] resources = resourcePatternResolver.getResources(metadataPath);
            List<Hot100Problem> loaded = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                try (InputStream inputStream = resource.getInputStream()) {
                    Hot100Problem problem = objectMapper.readValue(inputStream, Hot100Problem.class);
                    if (problem.getSlug() == null || problem.getSlug().isBlank()) {
                        continue;
                    }
                    problem.setMarkdownContent(loadMarkdown(markdownBasePath, problem.getSlug()));
                    loaded.add(problem);
                }
            }
            loaded.sort(Comparator.comparing(problem -> problem.getProblemId() == null ? Integer.MAX_VALUE : problem.getProblemId()));
            problemMap.clear();
            loaded.forEach(problem -> problemMap.put(problem.getSlug(), problem));
            log.info("Loaded {} Hot100 problems from {}", problemMap.size(), metadataPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load hot100 resources", e);
        }
    }

    private String loadMarkdown(String markdownBasePath, String slug) {
        String path = markdownBasePath + slug + ".md";
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            return "";
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            log.warn("Failed to load markdown for {}", slug, e);
            return "";
        }
    }
}
