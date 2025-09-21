package com.thinban.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求频率限制过滤器
 * 防止恶意请求频繁访问接口
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "rateLimitFilter")
@Order(2)
public class RateLimitFilter implements Filter {

    // 从配置文件读取限制参数：单位时间内最大请求数
    @Value("${security.rate-limit.max-requests:100}")
    private int maxRequests;

    // 时间窗口（秒）
    @Value("${security.rate-limit.window-seconds:60}")
    private int windowSeconds;

    // 存储客户端请求计数：key=客户端标识，value=计数和时间戳
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取客户端标识（优先使用X-Forwarded-For，其次RemoteAddr）
        String clientId = getClientId(httpRequest);

        // 检查是否超过请求限制
        if (isOverLimit(clientId)) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 获取客户端唯一标识
     */
    private String getClientId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 检查是否超过请求频率限制
     */
    private boolean isOverLimit(String clientId) {
        long now = System.currentTimeMillis() / 1000; // 当前时间（秒）
        RequestInfo info = requestCounts.get(clientId);

        if (info == null) {
            // 首次请求，初始化计数
            requestCounts.put(clientId, new RequestInfo(1, now));
            return false;
        }

        // 超出时间窗口，重置计数
        if (now - info.startTime > windowSeconds) {
            info.count.set(1);
            info.startTime = now;
            return false;
        }

        // 未超出时间窗口，计数+1
        int currentCount = info.count.incrementAndGet();
        return currentCount > maxRequests;
    }

    /**
     * 请求信息封装类
     */
    private static class RequestInfo {
        AtomicInteger count; // 请求计数
        long startTime; // 时间窗口起始时间（秒）

        RequestInfo(int count, long startTime) {
            this.count = new AtomicInteger(count);
            this.startTime = startTime;
        }
    }
}
