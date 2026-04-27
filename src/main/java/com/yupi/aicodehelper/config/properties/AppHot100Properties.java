package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.hot100")
public class AppHot100Properties {

    private String metadataPath = "classpath:hot100/json/*.json";

    private String markdownBasePath = "classpath:hot100/markdown/";
}
