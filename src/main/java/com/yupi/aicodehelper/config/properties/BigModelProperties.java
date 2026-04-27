package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bigmodel")
public class BigModelProperties {

    /**
     * BigModel integration master switch.
     */
    private boolean enabled = false;

    /**
     * Optional API key. Can be empty when BigModel is disabled.
     */
    private String apiKey;
}
