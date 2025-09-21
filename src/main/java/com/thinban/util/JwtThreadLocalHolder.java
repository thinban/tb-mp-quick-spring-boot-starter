package com.thinban.util;

public class JwtThreadLocalHolder {
    // ThreadLocal：每个线程独立存储，避免线程安全问题
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 存入用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 读取当前用户ID（核心方法：其他地方直接调用此方法获取）
     *
     * @return 当前请求的用户ID
     */
    public static Long getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除ThreadLocal（必须在请求结束后调用，避免内存泄漏）
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
