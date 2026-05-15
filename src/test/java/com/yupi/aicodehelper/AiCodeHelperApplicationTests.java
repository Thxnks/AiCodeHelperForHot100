package com.yupi.aicodehelper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:context_load_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.mcp.enabled=false",
        "app.rag.enabled=false",
        "langchain4j.community.dashscope.chat-model.api-key=test-key",
        "langchain4j.community.dashscope.streaming-chat-model.api-key=test-key",
        "langchain4j.community.dashscope.embedding-model.api-key=test-key"
})
class AiCodeHelperApplicationTests {

    @Test
    void contextLoads() {
    }

}
