package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mcp")
public class AppMcpProperties {

    private boolean enabled = false;

    /**
     * MCP server SSE endpoint. Keep empty when no MCP server is configured.
     */
    private String sseUrl;

    /**
     * API key used by the MCP server. Defaults to DASHSCOPE_API_KEY in application.yml.
     */
    private String apiKey;

    private long reconnectIntervalSeconds = 30;

    /**
     * Tool name exposed by the configured MCP server for web search.
     */
    private String webSearchToolName = "web_search";

    /**
     * Argument name used by the MCP web search tool.
     */
    private String webSearchQueryArgument = "query";
}
