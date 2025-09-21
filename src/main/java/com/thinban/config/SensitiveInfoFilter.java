package com.thinban.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息过滤过滤器
 * 过滤响应中的敏感信息（如手机号、身份证号等）
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "sensitiveInfoFilter")
@Order(4)
public class SensitiveInfoFilter implements Filter {

    @Autowired
    private ObjectMapper objectMapper;

    // 敏感信息正则匹配器
    private static final Map<Pattern, String> SENSITIVE_PATTERNS = new HashMap<>();

    static {
        // 手机号：保留前3后4位
        SENSITIVE_PATTERNS.put(Pattern.compile("1[3-9]\\d{9}"), "*** **** ****");
        // 身份证号：保留前6后4位
        SENSITIVE_PATTERNS.put(Pattern.compile("\\d{17}[\\dXx]"), "****** **** **** ****");
        // 银行卡号：保留后4位
        SENSITIVE_PATTERNS.put(Pattern.compile("\\d{16,19}"), "****************");
        // 邮箱：隐藏@前的部分字符
        SENSITIVE_PATTERNS.put(Pattern.compile("(\\w+)(@\\w+\\.\\w+)"), "$1***$2");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 包装响应对象，用于捕获输出内容
        SensitiveResponseWrapper responseWrapper = new SensitiveResponseWrapper((HttpServletResponse) response);

        chain.doFilter(request, responseWrapper);

        // 获取原始响应内容并过滤敏感信息
        String originalContent = responseWrapper.getContent();
        if (StringUtils.hasText(originalContent)) {
            String filteredContent = filterSensitiveInfo(originalContent);

            // 将过滤后的内容写入实际响应
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setContentLength(filteredContent.getBytes().length);
            PrintWriter out = httpResponse.getWriter();
            out.write(filteredContent);
            out.flush();
        }
    }

    /**
     * 过滤敏感信息
     */
    private String filterSensitiveInfo(String content) {
        String result = content;
        for (Map.Entry<Pattern, String> entry : SENSITIVE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(result);
            result = matcher.replaceAll(entry.getValue());
        }
        return result;
    }

    /**
     * 响应包装类：捕获响应内容
     */
    private static class SensitiveResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final ServletOutputStreamWrapper outputStreamWrapper;
        private PrintWriter writer;

        public SensitiveResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
            this.outputStreamWrapper = new ServletOutputStreamWrapper();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return outputStreamWrapper;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                writer = new PrintWriter(outputStreamWrapper);
            }
            return writer;
        }

        /**
         * 获取响应内容
         */
        public String getContent() {
            if (writer != null) {
                writer.flush();
            }
            return outputStreamWrapper.getContent();
        }
    }

    /**
     * Servlet输出流包装类：捕获输出内容
     */
    private static class ServletOutputStreamWrapper extends ServletOutputStream {
        private final StringBuilder content = new StringBuilder();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            // 无需实现
        }

        @Override
        public void write(int b) {
            content.append((char) b);
        }

        public String getContent() {
            return content.toString();
        }
    }
}
