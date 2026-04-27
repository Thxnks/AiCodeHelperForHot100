package com.yupi.aicodehelper.ai.mcp;

import com.yupi.aicodehelper.config.properties.AppMcpProperties;
import com.yupi.aicodehelper.config.properties.BigModelProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BigModelCapabilityService {

    private final BigModelProperties bigModelProperties;

    private final AppMcpProperties appMcpProperties;

    public BigModelCapabilityService(BigModelProperties bigModelProperties, AppMcpProperties appMcpProperties) {
        this.bigModelProperties = bigModelProperties;
        this.appMcpProperties = appMcpProperties;
    }

    public String buildCapabilityNotice() {
        boolean mcpEnabled = appMcpProperties.isEnabled();
        boolean bigModelEnabled = bigModelProperties.isEnabled();
        boolean hasApiKey = StringUtils.hasText(bigModelProperties.getApiKey());

        if (mcpEnabled && bigModelEnabled && hasApiKey) {
            return "BigModel 相关能力可用：MCP/Web 搜索已启用。";
        }
        return "BigModel 未配置，当前功能不可用：MCP/Web 搜索能力已禁用。";
    }
}
