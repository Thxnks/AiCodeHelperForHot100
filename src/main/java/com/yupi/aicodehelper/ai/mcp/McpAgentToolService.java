package com.yupi.aicodehelper.ai.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.config.properties.AppMcpProperties;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class McpAgentToolService {

    private final AppMcpProperties appMcpProperties;
    private final ObjectProvider<McpClient> mcpClientProvider;
    private final ObjectMapper objectMapper;

    public McpAgentToolService(AppMcpProperties appMcpProperties,
                               ObjectProvider<McpClient> mcpClientProvider,
                               ObjectMapper objectMapper) {
        this.appMcpProperties = appMcpProperties;
        this.mcpClientProvider = mcpClientProvider;
        this.objectMapper = objectMapper;
    }

    public McpWebSearchResult searchWeb(String query) {
        if (!appMcpProperties.isEnabled()) {
            return McpWebSearchResult.disabled("MCP is disabled by app.mcp.enabled=false");
        }
        if (!StringUtils.hasText(appMcpProperties.getApiKey())) {
            return McpWebSearchResult.disabled("Qwen/DashScope MCP API key is not configured");
        }
        if (!StringUtils.hasText(appMcpProperties.getSseUrl())) {
            return McpWebSearchResult.disabled("MCP SSE URL is not configured");
        }
        if (!StringUtils.hasText(query)) {
            return McpWebSearchResult.failed("Search query is blank");
        }

        McpClient mcpClient = mcpClientProvider.getIfAvailable();
        if (mcpClient == null) {
            return McpWebSearchResult.disabled("No MCP client bean is available");
        }

        List<String> availableTools = listToolNames(mcpClient);
        String toolName = chooseToolName(availableTools);
        Map<String, Object> arguments = Map.of(appMcpProperties.getWebSearchQueryArgument(), query.trim());

        try {
            String content = mcpClient.executeTool(ToolExecutionRequest.builder()
                    .name(toolName)
                    .arguments(toJson(arguments))
                    .build());
            return McpWebSearchResult.success(toolName, availableTools, arguments, content);
        } catch (Exception e) {
            return McpWebSearchResult.failed(toolName, availableTools, arguments, e.getMessage());
        }
    }

    private List<String> listToolNames(McpClient mcpClient) {
        try {
            return mcpClient.listTools().stream()
                    .map(ToolSpecification::name)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String chooseToolName(List<String> availableTools) {
        String configuredTool = appMcpProperties.getWebSearchToolName();
        if (StringUtils.hasText(configuredTool)) {
            return configuredTool.trim();
        }
        return availableTools.stream()
                .filter(name -> name.toLowerCase().contains("search"))
                .findFirst()
                .orElse("web_search");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    public record McpWebSearchResult(
            boolean available,
            boolean success,
            String toolName,
            List<String> availableTools,
            Map<String, Object> arguments,
            String content,
            String message
    ) {
        static McpWebSearchResult disabled(String message) {
            return new McpWebSearchResult(false, false, null, List.of(), Map.of(), null, message);
        }

        static McpWebSearchResult success(String toolName,
                                          List<String> availableTools,
                                          Map<String, Object> arguments,
                                          String content) {
            return new McpWebSearchResult(true, true, toolName, availableTools, arguments, content, null);
        }

        static McpWebSearchResult failed(String message) {
            return new McpWebSearchResult(true, false, null, List.of(), Map.of(), null, message);
        }

        static McpWebSearchResult failed(String toolName,
                                         List<String> availableTools,
                                         Map<String, Object> arguments,
                                         String message) {
            return new McpWebSearchResult(true, false, toolName, availableTools, arguments, null, message);
        }
    }
}
