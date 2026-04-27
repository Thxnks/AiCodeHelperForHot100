package com.yupi.aicodehelper;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiCodeHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeHelperApplication.class, args);
    }

}
