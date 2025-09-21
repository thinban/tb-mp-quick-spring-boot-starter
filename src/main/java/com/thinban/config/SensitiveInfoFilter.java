package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息过滤过滤器（修复显示格式问题）
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "sensitiveInfoFilter")
@Order(4)
public class SensitiveInfoFilter implements Filter {

    @Autowired
    private ObjectMapper objectMapper;

    // 敏感信息正则匹配器（修复替换规则）
    private static final Map<Pattern, String> SENSITIVE_PATTERNS = new HashMap<>();

    static {
        // 手机号：保留前3位和后4位，中间用*替换（138****1234）
        // 正则说明：捕获前3位(1[3-9]\\d)和后4位(\\d{4})，中间4位替换为*
        SENSITIVE_PATTERNS.put(Pattern.compile("(1[3-9]\\d)(\\d{4})(\\d{4})"), "$1****$3");

        // 身份证号：保留前6位和后4位，中间用*替换（110101********1234）
        // 正则说明：捕获前6位(\\d{6})和后4位(\\d{4}|X|x)，中间10位替换为*
        SENSITIVE_PATTERNS.put(Pattern.compile("(\\d{6})(\\d{8})(\\d{4}|X|x)"), "$1********$3");

        // 银行卡号：保留后4位（************1234）
        SENSITIVE_PATTERNS.put(Pattern.compile("(\\d{12,15})(\\d{4})"), "************$2");

        // 邮箱：保留前2位和域名，中间用*替换（ab***@example.com）
        SENSITIVE_PATTERNS.put(Pattern.compile("(\\w{2})(\\w+)(@\\w+\\.\\w+)"), "$1***$3");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        SensitiveResponseWrapper responseWrapper = new SensitiveResponseWrapper(httpResponse);
        chain.doFilter(request, responseWrapper);

        String originalContent = responseWrapper.getContent();
        if (StringUtils.hasText(originalContent)) {
            String filteredContent = filterSensitiveInfo(originalContent);

            try (PrintWriter out = httpResponse.getWriter()) {
                httpResponse.setContentLength(filteredContent.getBytes(StandardCharsets.UTF_8).length);
                out.write(filteredContent);
                out.flush();
            }
        }
    }

    /**
     * 过滤敏感信息（使用分组捕获保留需要显示的部分）
     */
    private String filterSensitiveInfo(String content) {
        String result = content;
        for (Map.Entry<Pattern, String> entry : SENSITIVE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(result);
            // 使用分组替换，保留需要显示的部分
            result = matcher.replaceAll(entry.getValue());
        }
        return result;
    }

    /**
     * 响应包装类：使用UTF-8编码捕获响应内容
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
                writer = new PrintWriter(new OutputStreamWriter(outputStreamWrapper, StandardCharsets.UTF_8));
            }
            return writer;
        }

        public String getContent() {
            if (writer != null) {
                writer.flush();
            }
            return new String(outputStreamWrapper.getBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 输出流包装类：使用字节数组存储内容
     */
    private static class ServletOutputStreamWrapper extends jakarta.servlet.ServletOutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

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
            buffer.write(b);
        }

        public byte[] getBytes() {
            return buffer.toByteArray();
        }
    }
}
