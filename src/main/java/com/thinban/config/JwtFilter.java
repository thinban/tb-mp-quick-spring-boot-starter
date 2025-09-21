package com.thinban.config;

import cn.hutool.core.convert.NumberWithFormat;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinban.core.R;
import com.thinban.util.JwtThreadLocalHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT过滤器：基于@WebFilter注解配置，替代原拦截器功能
 */
@Component
@WebFilter(urlPatterns = "/*",  // 拦截所有请求
        filterName = "jwtFilter", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD}, initParams = {@WebInitParam(name = "encoding", value = "UTF-8"), // 编码格式
//                @WebInitParam(name = "excludePaths", value = "/login,/register") // 忽略拦截的路径
})
@Order(10) // 过滤器执行顺序（数值越小越先执行）
public class JwtFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${tb.jwt.key}")
    String key;
    // 从配置文件读取需要跳过的路径列表
    @Value("${tb.jwt.excludes:}")
    private List<String> excludePaths;

    // 注入Spring自动配置的ObjectMapper
    @Resource
    private ObjectMapper objectMapper;

    // 路径匹配器（Spring自带的Ant风格路径匹配工具）
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 转换为HTTP请求/响应对象
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // 0. 检查当前请求路径是否在排除列表中
            if (isSkipJwtVerify(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1. 从请求头获取Token（格式：Bearer {token}）
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("未获取到有效Token:{}", request.getRequestURI());
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未获取到有效Token");
                sendJsonError(response, "未获取到有效Token");
                return;
            }

            // 2. 截取Token（去除"Bearer "前缀）
            String token = authHeader.substring(7);

            // 3. 验证Token有效性
            if (!JWTUtil.verify(token, key.getBytes(StandardCharsets.UTF_8))) {
                log.error("Token验证失败:{},{}", request.getRequestURI(), token);
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token验证失败");
                sendJsonError(response, "Token验证失败");
                return;
            }

            // 4. 解析Token获取用户ID并存入ThreadLocal
            JWT jwt = JWTUtil.parseToken(token);
            Long userId = ((NumberWithFormat) jwt.getPayload("id")).longValue();
            JwtThreadLocalHolder.setUserId(userId);

            // 5. 放行请求（继续执行后续过滤器或目标资源）
            filterChain.doFilter(request, response);
        } finally {
            // 6. 清除ThreadLocal（无论请求成功与否都需清理，避免内存泄漏）
            JwtThreadLocalHolder.clear();
        }
    }

    // 初始化和销毁方法（如需初始化资源可重写）
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    /**
     * 判断当前请求是否需要跳过JWT验证
     * 从配置文件读取tb.jwt.excludes，支持Ant风格路径模糊匹配
     */
    private boolean isSkipJwtVerify(HttpServletRequest request) {
        // 获取当前请求路径（如：/api/login）
        String requestPath = request.getRequestURI();

        // 遍历配置的排除路径，进行模糊匹配
        for (String excludePath : excludePaths) {
            // 支持Ant风格匹配：?匹配单个字符，*匹配多级字符，**匹配多级路径
            if (pathMatcher.match(excludePath, requestPath)) {
                return true;
            }
        }

        // 不匹配任何排除路径，需要验证
        return false;
    }

    /**
     * 发送JSON格式的错误响应
     *
     * @param response 响应对象
     * @param message  错误消息
     */
    private void sendJsonError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        R fail = R.fail(message, null);
        response.getWriter().write(objectMapper.writeValueAsString(fail));
    }
}