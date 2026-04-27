package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mcp")
public class AppMcpProperties {

    private boolean enabled = false;

    private String sseUrl = "https://open.bigmodel.cn/api/mcp/web_search/sse";

    private long reconnectIntervalSeconds = 30;
}
