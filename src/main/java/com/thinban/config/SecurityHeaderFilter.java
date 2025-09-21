package com.thinban.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * 添加增强安全性的HTTP响应头
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "securityHeaderFilter")
@Order(3)
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 内容安全策略（CSP）：限制资源加载来源
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");

        // 2. 防止XSS攻击：启用浏览器内置XSS过滤
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 3. 防止点击劫持：禁止iframe嵌套
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 4. 防止MIME类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 5. 限制Referrer信息泄露
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 6. 禁止浏览器自动填充密码（可选）
        httpResponse.setHeader("Cache-Control", "no-store");
        httpResponse.setHeader("Pragma", "no-cache");

        // 7. 限制HTTP方法（可选）
        httpResponse.setHeader("Allow", "GET, POST, PUT, DELETE, OPTIONS");

        chain.doFilter(request, response);
    }
}
