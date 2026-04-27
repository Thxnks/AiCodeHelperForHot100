package com.yupi.aicodehelper.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TraceIdFilter traceIdFilter;

    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          TraceIdFilter traceIdFilter,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.traceIdFilter = traceIdFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/meta/error-codes",
                                "/api/meta/error-codes",
                                "/v3/api-docs/**",
                                "/api/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/swagger-ui.html",
                                "/actuator/health",
                                "/api/actuator/health",
                                "/actuator/info",
                                "/api/actuator/info",
                                "/health",
                                "/api/health"
                        ).permitAll()
                        .requestMatchers("/actuator/prometheus", "/api/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers("/ai/chat", "/api/ai/chat").permitAll()
                        .requestMatchers("/roles/**", "/api/roles/**").permitAll()
                        .requestMatchers("/hot100/dataset-stats", "/api/hot100/dataset-stats").hasRole("ADMIN")
                        .requestMatchers("/hot100/problems", "/hot100/problems/**", "/hot100/tags",
                                "/api/hot100/problems", "/api/hot100/problems/**", "/api/hot100/tags").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN)))
                .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, TraceIdFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.error(errorCode)));
    }
}
