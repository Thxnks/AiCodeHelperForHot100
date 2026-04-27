package com.yupi.aicodehelper.ai.mcp;

import com.yupi.aicodehelper.config.properties.AppMcpProperties;
import com.yupi.aicodehelper.config.properties.BigModelProperties;
import com.yupi.aicodehelper.config.condition.BigModelApiKeyPresentCondition;
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
@ConditionalOnProperty(prefix = "bigmodel", name = "enabled", havingValue = "true")
@Conditional(BigModelApiKeyPresentCondition.class)
public class McpConfig {

    private final BigModelProperties bigModelProperties;

    private final AppMcpProperties appMcpProperties;

    public McpConfig(BigModelProperties bigModelProperties, AppMcpProperties appMcpProperties) {
        this.bigModelProperties = bigModelProperties;
        this.appMcpProperties = appMcpProperties;
    }

    @Bean
    public McpToolProvider mcpToolProvider() {
        String apiKey = bigModelProperties.getApiKey();
        String sseUrl = appMcpProperties.getSseUrl();
        long reconnectIntervalSeconds = appMcpProperties.getReconnectIntervalSeconds();

        String encodedAuthorization = URLEncoder.encode("Bearer " + apiKey, StandardCharsets.UTF_8);
        String fullSseUrl = sseUrl + (sseUrl.contains("?") ? "&" : "?") + "Authorization=" + encodedAuthorization;

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(fullSseUrl)
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key("yupiMcpClient")
                .transport(transport)
                .reconnectInterval(Duration.ofSeconds(reconnectIntervalSeconds))
                .build();

        return McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
    }
}
