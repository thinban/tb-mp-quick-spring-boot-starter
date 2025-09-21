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
 * 敏感信息过滤过滤器（修复中文乱码问题）
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
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // 1. 强制设置响应编码为UTF-8（关键：解决中文乱码）
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 2. 使用带编码的响应包装器
        SensitiveResponseWrapper responseWrapper = new SensitiveResponseWrapper(httpResponse);

        // 3. 继续执行过滤器链
        chain.doFilter(request, responseWrapper);

        // 4. 处理响应内容
        String originalContent = responseWrapper.getContent();
        if (StringUtils.hasText(originalContent)) {
            String filteredContent = filterSensitiveInfo(originalContent);

            // 5. 写入处理后的内容（使用正确编码）
            try (PrintWriter out = httpResponse.getWriter()) {
                httpResponse.setContentLength(filteredContent.getBytes(StandardCharsets.UTF_8).length);
                out.write(filteredContent);
                out.flush();
            }
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
     * 响应包装类：使用UTF-8编码捕获响应内容
     */
    private static class SensitiveResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final ServletOutputStreamWrapper outputStreamWrapper;
        private PrintWriter writer;

        public SensitiveResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
            // 关键：使用UTF-8编码的字节流
            this.outputStreamWrapper = new ServletOutputStreamWrapper();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return outputStreamWrapper;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                // 关键：指定UTF-8编码创建PrintWriter
                writer = new PrintWriter(new OutputStreamWriter(outputStreamWrapper, StandardCharsets.UTF_8));
            }
            return writer;
        }

        /**
         * 获取响应内容（使用UTF-8解码）
         */
        public String getContent() {
            if (writer != null) {
                writer.flush();
            }
            return new String(outputStreamWrapper.getBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 输出流包装类：使用字节数组存储内容，避免编码问题
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

        /**
         * 获取原始字节数组
         */
        public byte[] getBytes() {
            return buffer.toByteArray();
        }
    }
}
