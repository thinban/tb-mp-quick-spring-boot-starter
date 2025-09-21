package com.thinban.monitor;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC工具
 * <p>
 * // 提交任务时复制当前 MDC 上下文
 * String traceId = TraceIdUtils.getTraceId();
 * executorService.submit(() -> {
 * try {
 * TraceIdUtils.setTraceId(traceId); // 手动设置 traceId 到子线程
 * // 异步处理逻辑
 * } finally {
 * TraceIdUtils.clearTraceId(); // 清理
 * }
 * });
 */
public class TraceIdUtils {
    // MDC 中 traceId 的键名
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 生成全局唯一的 traceId（UUID 去掉横线）
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


    /**
     * 设置 traceId 到 MDC
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 初始化
     */
    public static void initTraceId() {
        setTraceId(generateTraceId());
    }

    /**
     * 从 MDC 获取 traceId（若不存在则返回默认值）
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY) == null ? "unknown_trace_id" : MDC.get(TRACE_ID_KEY);
    }

    /**
     * 清除 MDC 中的 traceId（避免线程池复用导致的污染）
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}