package com.yupi.aicodehelper.ai;

import com.alibaba.dashscope.assistants.Assistant;
import com.yupi.aicodehelper.ai.tools.Hot100Tool;
import com.yupi.aicodehelper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    @Resource
    private ObjectProvider<ContentRetriever> contentRetrieverProvider;

    @Resource
    private ObjectProvider<McpToolProvider> mcpToolProviderProvider;

    @Resource
    private Hot100Tool hot100Tool;

    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        // 构造 AI Service
        var builder = AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(10)) // 每个会话独立存储
                .tools(new InterviewQuestionTool(), hot100Tool) // 工具调用
                ;
        ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever); // RAG 检索增强生成
        }
        McpToolProvider mcpToolProvider = mcpToolProviderProvider.getIfAvailable();
        if (mcpToolProvider != null) {
            builder.toolProvider(mcpToolProvider); // MCP 工具调用
        }
        return builder.build();
    }
}
