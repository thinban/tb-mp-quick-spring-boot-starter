package com.thinban.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS过滤器：过滤请求参数中的恶意脚本
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "xssFilter")
@Order(0) // 优先级高于JWT过滤器，先进行XSS过滤
public class XssFilter implements Filter {

    // XSS攻击模式匹配正则
    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 使用包装类处理请求参数
        chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
    }

    /**
     * HTTP请求包装类：重写参数获取方法，过滤XSS内容
     */
    private static class XssHttpServletRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        // 过滤单个参数值
        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return stripXss(value);
        }

        // 过滤数组参数
        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = stripXss(values[i]);
                }
            }
            return values;
        }

        // 过滤请求头
        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return stripXss(value);
        }
    }

    /**
     * 移除XSS攻击脚本
     */
    private static String stripXss(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        // 替换特殊字符
        String cleanValue = value;
        for (Pattern pattern : XSS_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }
        // 转义HTML特殊字符
        cleanValue = cleanValue.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("&", "&amp;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#39;");
        return cleanValue;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
