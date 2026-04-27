package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.roles")
public class AppRolesProperties {

    private String defaultRoleId = "assistant";

    private String configPath = "classpath:roles/*.json";
}
