package com.luoye.aspect;

import com.luoye.annotation.OperationLogger;
import com.luoye.context.BaseContext;
import com.luoye.entity.Log;
import com.luoye.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private LogService logService;

    @Autowired
    private HttpServletRequest request;

    @Around("@annotation(operationLogger)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogger operationLogger) throws Throwable {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        Log log = buildBasicLog(operationLogger, currentTime);

        try {
            // 执行原方法
            Object result = joinPoint.proceed();

            // 设置操作成功标志
            log.setSuccessFlag(1);
            return result;
        } catch (Throwable throwable) {
            // 设置操作失败标志和错误消息
            log.setSuccessFlag(0);
            log.setErrorMessage(throwable.getMessage());
            throw throwable;
        } finally {
            // 设置操作详情
            setOperationDetails(joinPoint, log);
            // 设置操作人信息
            setOperatorInfo(log);
            // 获取IP地址
            log.setIpAddress(getIpAddress());
            // 保存日志
            logService.save(log);
        }
    }

    /**
     * 构建基础日志对象
     */
    private Log buildBasicLog(OperationLogger operationLogger, LocalDateTime currentTime) {
        Log log = new Log();
        log.setLogType(operationLogger.operationType());
        log.setTargetType(operationLogger.targetType());
        log.setCreateTime(currentTime);
        return log;
    }

    /**
     * 设置操作详情
     */
    private void setOperationDetails(ProceedingJoinPoint joinPoint, Log log) {
        try {
            StringBuilder detail = new StringBuilder();
            // 添加被调用的方法信息
            detail.append("方法: ")
                    .append(joinPoint.getSignature().getDeclaringTypeName())
                    .append(".")
                    .append(joinPoint.getSignature().getName())
                    .append("\n");

            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 添加参数信息到详情中，避免敏感信息泄露
                detail.append("参数数量: ").append(args.length);
            }

            log.setOperationDetail(detail.toString());
        } catch (Exception e) {
            log.setOperationDetail("无法解析操作详情: " + e.getMessage());
        }
    }

    /**
     * 设置操作人信息
     */
    private void setOperatorInfo(Log log) {
        try {
            // 从BaseContext获取用户信息
            Long userId = BaseContext.getCurrentId();
            String identity = BaseContext.getCurrentIdentity();

            if (userId != null && identity != null) {
                log.setOperatorId(userId);
                log.setOperatorType(identity);

                // 根据身份类型设置操作人名称
                switch (identity.toUpperCase()) {
                    case "PATIENT":
                        log.setOperatorName("患者_" + userId);
                        break;
                    case "DOCTOR":
                        log.setOperatorName("医生_" + userId);
                        break;
                    case "ADMIN":
                        log.setOperatorName("管理员_" + userId);
                        break;
                    default:
                        log.setOperatorName("未知_" + userId);
                        break;
                }
            } else {
                log.setOperatorName("匿名用户");
                log.setOperatorType("ANONYMOUS");
            }
        } catch (Exception e) {
            log.setOperatorName("未知用户");
            log.setOperatorType("UNKNOWN");
        }
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
