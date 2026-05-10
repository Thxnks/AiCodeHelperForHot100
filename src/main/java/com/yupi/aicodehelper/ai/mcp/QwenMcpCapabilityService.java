package com.yupi.aicodehelper.ai.mcp;

import com.yupi.aicodehelper.config.properties.AppMcpProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QwenMcpCapabilityService {

    private final AppMcpProperties appMcpProperties;

    public QwenMcpCapabilityService(AppMcpProperties appMcpProperties) {
        this.appMcpProperties = appMcpProperties;
    }

    public String buildCapabilityNotice() {
        boolean mcpEnabled = appMcpProperties.isEnabled();
        boolean hasApiKey = StringUtils.hasText(appMcpProperties.getApiKey());
        boolean hasSseUrl = StringUtils.hasText(appMcpProperties.getSseUrl());

        if (mcpEnabled && hasApiKey && hasSseUrl) {
            return "Qwen/DashScope MCP capability is available: MCP web search is enabled.";
        }
        return "Qwen/DashScope MCP capability is disabled or incomplete: MCP web search is unavailable.";
    }
}
