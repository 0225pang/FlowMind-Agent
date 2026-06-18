package com.flowmind.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final SecurityPermissionService permissionService;

    public AuthInterceptor(SecurityPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (isPublic(uri)) {
            return true;
        }

        Optional<String> username = TokenUtil.parseUsername(request.getHeader("Authorization"));
        if (username.isEmpty()) {
            write(response, 401, "missing or invalid token");
            return false;
        }

        SecurityPermissionService.AccessDecision decision = permissionService.decide(username.get(), request.getMethod(), uri);
        if (!decision.allowed()) {
            write(response, 403, decision.message());
            return false;
        }

        request.setAttribute("currentUsername", username.get());
        request.setAttribute("currentRoles", decision.roles());
        request.setAttribute("currentPermissions", decision.permissions());
        return true;
    }

    private boolean isPublic(String uri) {
        return uri.startsWith("/api/auth")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/actuator/health")
                || uri.equals("/error");
    }

    private void write(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"code":%d,"message":"%s","data":null}
                """.formatted(status, escape(message)));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
