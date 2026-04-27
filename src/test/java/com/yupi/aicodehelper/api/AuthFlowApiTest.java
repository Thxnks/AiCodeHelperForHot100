package com.yupi.aicodehelper.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_flow_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AuthFlowApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteAuthLifecycleAndRejectReusedRefreshToken() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "user_" + suffix;
        String email = username + "@example.com";
        String password = "12345678";

        JsonNode registerData = extractData(register(username, email, password));
        String accessToken = registerData.path("accessToken").asText();
        String refreshToken = registerData.path("refreshToken").asText();

        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username));

        JsonNode refreshData = extractData(refresh(refreshToken));
        String refreshedAccessToken = refreshData.path("accessToken").asText();
        String refreshedRefreshToken = refreshData.path("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40103));

        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        MvcResult postLogoutRefresh = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshedRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode postLogoutRefreshBody = parseBody(postLogoutRefresh);
        int postLogoutCode = postLogoutRefreshBody.path("code").asInt();
        assertTrue(Set.of(40101, 40103).contains(postLogoutCode));
    }

    @Test
    void shouldReturn403WhenUserAccessesAdminEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "user_" + suffix;
        String email = username + "@example.com";
        String password = "12345678";

        JsonNode registerData = extractData(register(username, email, password));
        String accessToken = registerData.path("accessToken").asText();

        mockMvc.perform(get("/hot100/dataset-stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    private MvcResult register(String username, String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }

    private JsonNode extractData(MvcResult result) throws Exception {
        return parseBody(result).path("data");
    }

    private JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
