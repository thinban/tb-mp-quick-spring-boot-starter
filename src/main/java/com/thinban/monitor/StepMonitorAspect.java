package com.thinban.monitor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

// 标记为切面组件
@Aspect
@Component
public class StepMonitorAspect {
    private static final Logger log = LoggerFactory.getLogger(StepMonitorAspect.class);

    // 拦截所有被 @StepMonitor 注解的方法
    @Around("@annotation(com.thinban.monitor.StepMonitor)") // 替换为你的注解全路径
    public Object monitorStep(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息和上下文
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        StepMonitor stepMonitor = method.getAnnotation(StepMonitor.class);
        String stepName = stepMonitor.stepName();
        String threadId = String.valueOf(Thread.currentThread().getId());
        String msgId = getMsgIdFromArgs(joinPoint.getArgs());

        // 2. 从 MDC 获取 traceId（核心：关联整个链路）
        String traceId = TraceIdUtils.getTraceId();

        // 3. 步骤开始日志：包含 traceId
        long startTime = System.currentTimeMillis();
//        log.info("[StepMonitor] traceId={}, step={}, action=start, threadId={}, msgId={}, method={}",
//                traceId, stepName, threadId, msgId, method.getName());

        Object result = null;
        try {
            result = joinPoint.proceed();
            // 4. 成功日志：包含 traceId 和耗时
            long costTime = System.currentTimeMillis() - startTime;
            log.info("[StepMonitor] step={}, action=success, threadId={},costTime={}ms",
                    stepName, threadId, costTime);
            return result;
        } catch (Exception e) {
            // 5. 异常日志：包含 traceId 和异常信息
            long costTime = System.currentTimeMillis() - startTime;
            log.error("[StepMonitor]  step={}, action=error, threadId={}, costTime={}ms, errorMsg={}",
                    stepName, threadId, costTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 辅助方法：从方法参数中提取MQ消息ID（需根据你的消息实体结构调整）
     * 示例：若参数是自定义Msg类，且有getId()方法，则通过反射获取
     */
    private String getMsgIdFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "unknown_msg_id";
        }
        // 假设第一个参数是MQ消息实体，且有getId()方法
        Object msgObj = args[0];
        try {
            Method getIdMethod = msgObj.getClass().getMethod("getId");
            return String.valueOf(getIdMethod.invoke(msgObj));
        } catch (Exception e) {
            return "parse_msg_id_fail";
        }
    }
}
