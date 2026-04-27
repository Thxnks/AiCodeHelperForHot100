package com.yupi.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true")
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {
        // 1) Load RAG documents from classpath so it works in IDE and Docker/JAR runtime.
        List<Document> documents = loadClasspathDocuments("classpath:docs/**/*");

        // 2) Split documents into paragraphs.
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(1000, 200);

        // 3) Ingest documents into embedding store.
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);

        // 4) Build content retriever.
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }

    private List<Document> loadClasspathDocuments(String resourcePattern) {
        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(null);
            org.springframework.core.io.Resource[] resources = resolver.getResources(resourcePattern);
            List<Document> documents = new ArrayList<>();
            for (org.springframework.core.io.Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                if (text.isBlank()) {
                    continue;
                }
                Metadata metadata = Metadata.from(Document.FILE_NAME, filename);
                documents.add(Document.from(text, metadata));
            }
            return documents;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RAG documents from classpath", e);
        }
    }
}
