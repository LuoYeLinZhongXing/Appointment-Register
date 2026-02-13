package com.luoye.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 缓存命中监控切面
 * 监控所有带有@Cacheable注解的方法，输出缓存命中日志
 */
@Aspect
@Component
public class CacheMonitorAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheMonitorAspect.class);

    /**
     * 监控所有@Cacheable注解的方法
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheHit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法信息
        Method method = getTargetMethod(joinPoint);
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 获取缓存注解信息
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        String cacheName = String.join(",", cacheable.value());

        // 记录方法调用开始 - 改为DEBUG级别
        long startTime = System.currentTimeMillis();
        logger.debug("🔍 开始缓存查询 - 方法: {}, 缓存名: {}", methodName, cacheName);

        try {
            // 执行原方法
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;

            // 判断是否命中缓存 - 改为DEBUG级别
            if (result != null) {
                logger.debug("✅ 缓存命中 - 方法: {}, 缓存名: {}, 耗时: {}ms, 返回结果类型: {}",
                           methodName, cacheName, executeTime, result.getClass().getSimpleName());
            } else {
                logger.debug("❌ 缓存未命中或数据为空 - 方法: {}, 缓存名: {}, 耗时: {}ms",
                           methodName, cacheName, executeTime);
            }

            return result;
        } catch (Exception e) {
            // 错误日志保持ERROR级别
            logger.error("❌ 缓存查询异常 - 方法: {}, 缓存名: {}, 异常: {}",
                        methodName, cacheName, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取目标方法
     */
    private Method getTargetMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] paramTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes();
        return targetClass.getMethod(methodName, paramTypes);
    }
}
