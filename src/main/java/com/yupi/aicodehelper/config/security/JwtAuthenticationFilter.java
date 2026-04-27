package com.yupi.aicodehelper.config.security;

import com.yupi.aicodehelper.auth.AuthUserPrincipal;
import com.yupi.aicodehelper.auth.JwtTokenService;
import com.yupi.aicodehelper.entity.UserAccount;
import com.yupi.aicodehelper.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserAccountRepository userAccountRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtTokenService.parseClaims(token);
                String tokenType = claims.get(JwtTokenService.CLAIM_TOKEN_TYPE, String.class);
                if (!JwtTokenService.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get(JwtTokenService.CLAIM_USERNAME, String.class);
                String role = claims.get(JwtTokenService.CLAIM_ROLE, String.class);
                Long tokenVersion = claims.get(JwtTokenService.CLAIM_TOKEN_VERSION, Long.class);
                UserAccount user = userAccountRepository.findById(userId).orElse(null);
                if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                Long currentTokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
                if (tokenVersion == null || !currentTokenVersion.equals(tokenVersion)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                String normalizedRole = normalizeRole(role, user.getRole());
                String effectiveUsername = (username == null || username.isBlank()) ? user.getUsername() : username;
                AuthUserPrincipal principal = new AuthUserPrincipal(userId, effectiveUsername, normalizedRole);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String roleFromToken, String roleFromDb) {
        String source = roleFromToken;
        if (source == null || source.isBlank()) {
            source = roleFromDb;
        }
        if (source == null || source.isBlank()) {
            return "USER";
        }
        return source.trim().toUpperCase();
    }
}
