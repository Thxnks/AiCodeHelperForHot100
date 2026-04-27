package com.yupi.aicodehelper.config.security;

import com.yupi.aicodehelper.auth.AuthUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "auditStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTime = request.getAttribute(START_TIME_ATTR);
        long costMs = startTime instanceof Long value ? System.currentTimeMillis() - value : -1L;
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        String username = "anonymous";
        String role = "ANON";
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
            userId = String.valueOf(principal.userId());
            username = principal.username();
            role = principal.role();
        }
        log.info("audit method={} path={} status={} costMs={} userId={} username={} role={} traceId={} ip={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                costMs,
                userId,
                username,
                role,
                traceId,
                request.getRemoteAddr());
    }
}
