package com.yupi.aicodehelper.agent;

import java.util.List;

public record KnowledgeSnippetView(
        String source,
        String slug,
        String title,
        String section,
        Integer score,
        List<String> matchedTerms,
        String content
) {
    public KnowledgeSnippetView(String source, String content) {
        this(source, null, null, null, null, List.of(), content);
    }
}
