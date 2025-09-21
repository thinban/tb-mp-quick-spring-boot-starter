package com.thinban.monitor;

import java.lang.annotation.*;

// 作用在方法上
@Target(ElementType.METHOD)
// 运行时生效（AOP需运行时拦截）
@Retention(RetentionPolicy.RUNTIME)
// 允许在文档中显示
@Documented
public @interface StepMonitor {
    // 步骤名称（必填，如"parse_msg"、"save_result"）
    String stepName();
}