package com.thinban.config;

import com.thinban.core.R;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;

/**
 * 全局异常处理器，统一处理 IllegalArgumentException 并返回 R<T>格式响应
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 注入环境变量，判断当前环境（dev/test/prod）
    @Resource
    private Environment environment;

    // 判断是否为开发或测试环境
    private boolean isDevOrTestEnv() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).stream()
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    // 简化堆栈信息（只保留前 n 行）
    private Throwable getSimplifiedStackTrace(Throwable ex, int maxLines) {
        if (ex == null || maxLines <= 0) {
            return ex;
        }
        // 复制异常对象，避免修改原始堆栈
        Throwable simplified = new Throwable(ex.getMessage());
        StackTraceElement[] original = ex.getStackTrace();
        // 截取前 maxLines 行堆栈
        StackTraceElement[] truncated = Arrays.copyOf(original,
                Math.min(original.length, maxLines));
        simplified.setStackTrace(truncated);
        return simplified;
    }

    /**
     * 处理 IllegalArgumentException 异常
     *
     * @param ex 捕获的异常对象
     * @return 封装后的异常响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public R handleIllegalArgumentException(IllegalArgumentException ex) {
        log.info("参数异常: {}", ex.getMessage(), getSimplifiedStackTrace(ex, 5));
        return R.fail(ex.getMessage(), null);
    }

    /**
     * 处理其他未捕获的异常（可选）
     *
     * @param ex 捕获的异常对象
     * @return 封装后的异常响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R handleGeneralException(Exception ex) {
        // 根据环境决定是否打印完整堆栈
        if (isDevOrTestEnv()) {
            // 开发/测试环境：打印 ERROR 级别完整堆栈
            log.error("系统异常: {}", ex.getMessage(), ex);
        } else {
            // 生产环境：打印 ERROR 级别，但只显示关键信息（避免日志过大）
            log.error("系统异常: {}", ex.getMessage(), getSimplifiedStackTrace(ex, 5));
        }

        return R.fail("系统繁忙", null);
    }
}