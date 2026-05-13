package com.yupi.aicodehelper.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentKnowledgeServiceTest {

    @Test
    void shouldRetrieveExplainableLocalRagChunks() {
        AgentKnowledgeService service = new AgentKnowledgeService(new EmptyObjectProvider<>(), new ObjectMapper());

        List<KnowledgeSnippetView> snippets = service.retrieve("coin change dynamic programming 完全背包", 3);

        assertThat(snippets).isNotEmpty();
        assertThat(snippets)
                .anySatisfy(snippet -> {
                    assertThat(snippet.slug()).isEqualTo("coin-change");
                    assertThat(snippet.title()).isEqualTo("零钱兑换");
                    assertThat(snippet.section()).isNotBlank();
                    assertThat(snippet.score()).isPositive();
                    assertThat(snippet.matchedTerms()).containsAnyOf("coin", "change", "dynamic", "programming", "完全背包");
                    assertThat(snippet.content()).contains("完全背包");
                });
    }

    private static class EmptyObjectProvider<T> implements ObjectProvider<T> {

        @Override
        public T getObject(Object... args) {
            return null;
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public T getObject() {
            return null;
        }

        @Override
        public Iterator<T> iterator() {
            return List.<T>of().iterator();
        }
    }
}
