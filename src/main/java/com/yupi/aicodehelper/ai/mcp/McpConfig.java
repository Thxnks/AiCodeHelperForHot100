package com.yupi.aicodehelper.ai.mcp;

import com.yupi.aicodehelper.config.properties.AppMcpProperties;
import com.yupi.aicodehelper.config.condition.AppMcpApiKeyPresentCondition;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "app.mcp", name = "enabled", havingValue = "true")
@Conditional(AppMcpApiKeyPresentCondition.class)
public class McpConfig {

    private final AppMcpProperties appMcpProperties;

    public McpConfig(AppMcpProperties appMcpProperties) {
        this.appMcpProperties = appMcpProperties;
    }

    @Bean
    public McpClient mcpClient() {
        String apiKey = appMcpProperties.getApiKey();
        String sseUrl = appMcpProperties.getSseUrl();
        long reconnectIntervalSeconds = appMcpProperties.getReconnectIntervalSeconds();

        String encodedAuthorization = URLEncoder.encode("Bearer " + apiKey, StandardCharsets.UTF_8);
        String fullSseUrl = sseUrl + (sseUrl.contains("?") ? "&" : "?") + "Authorization=" + encodedAuthorization;

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(fullSseUrl)
                .logRequests(true)
                .logResponses(true)
                .build();

        return new DefaultMcpClient.Builder()
                .key("yupiMcpClient")
                .transport(transport)
                .reconnectInterval(Duration.ofSeconds(reconnectIntervalSeconds))
                .build();
    }

    @Bean
    public McpToolProvider mcpToolProvider(McpClient mcpClient) {
        return McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
    }
}
