package com.luoye.interceptor;

import com.luoye.context.BaseContext;
import com.luoye.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * JWT拦截器
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);

    /**
     * 拦截请求，验证JWT令牌
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("JWT拦截器开始处理请求: {}", request.getRequestURI());

        // 从请求头中获取JWT令牌
        String token = request.getHeader("Authorization");
        log.info("从Authorization头获取token: {}", token != null ? "存在" : "不存在");

        // 如果Authorization头为空，尝试从token头获取
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
            log.info("从token头获取token: {}", token != null ? "存在" : "不存在");
        }

        // 如果是登录或注册请求，直接放行
        String requestUri = request.getRequestURI();
        // 明确的白名单路径
        if (requestUri.equals("/patient/login") ||
                requestUri.equals("/patient/register") ||
                requestUri.equals("/doctor/login") ||
                requestUri.equals("/doctor/register") ||
                requestUri.equals("/admin/login") ||
                requestUri.equals("/admin/register")) {
            logger.info("登录或注册请求，直接放行: {}", requestUri);
            return true;
        }

        // 检查令牌是否带有"Bearer "前缀，如果有则移除
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            log.info("从Bearer头获取token: {}", token != null ? "存在" : "不存在");
        }

        // 验证令牌是否存在
        if (token == null || token.isEmpty()) {
            log.warn("请求缺少JWT token: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"Unauthorized: Missing token\", \"data\": null}");
            return false;
        }

        // 验证 token 格式
        if (!JwtUtil.isValidTokenFormat(token)) {
            log.warn("JWT token格式无效: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"Unauthorized: Invalid token format\", \"data\": null}");
            return false;
        }

        try {
            // 解析JWT令牌
            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                log.warn("JWT token解析失败: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 401, \"msg\": \"Unauthorized: Invalid token\", \"data\": null}");
                return false;
            }

            log.info("JWT claims解析结果: {}", claims);
            // 设置用户信息
            Object patientIdObj = claims.get("patientId");
            Object doctorIdObj = claims.get("doctorId");
            Object adminIdObj = claims.get("adminId");


            // 添加调试信息
            log.info("JWT claims解析结果 - patientIdObj: {}, 类型: {}, 值: {}",
                    patientIdObj,
                    patientIdObj != null ? patientIdObj.getClass().getSimpleName() : "null",
                    patientIdObj);
            log.info("JWT claims解析结果 - doctorIdObj: {}, 类型: {}, 值: {}",
                    doctorIdObj,
                    doctorIdObj != null ? doctorIdObj.getClass().getSimpleName() : "null",
                    doctorIdObj);
            log.info("JWT claims解析结果 - adminIdObj: {}, 类型: {}, 值: {}",
                    adminIdObj,
                    adminIdObj != null ? adminIdObj.getClass().getSimpleName() : "null",
                    adminIdObj);

            // 安全地转换ID类型
            Long patientId = convertToLong(patientIdObj);
            Long doctorId = convertToLong(doctorIdObj);
            Long adminId = convertToLong(adminIdObj);

            if (patientId != null) {
                BaseContext.setCurrentId(patientId, "PATIENT");
                log.info("设置患者上下文: id={}, identity=PATIENT", patientId);
            } else if (doctorId != null) {
                BaseContext.setCurrentId(doctorId, "DOCTOR");
                log.info("设置医生上下文: id={}, identity=DOCTOR", doctorId);
            } else if (adminId != null) {
                BaseContext.setCurrentId(adminId, "ADMIN");
                log.info("设置管理员上下文: id={}, identity=ADMIN", adminId);
            } else {
                log.warn("JWT claims中未找到有效的用户信息: {}", request.getRequestURI());
                log.warn("claims内容: {}", claims);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\": 401, \"msg\": \"Unauthorized: No valid user info\", \"data\": null}");
                return false;
            }
            log.info("JWT验证成功: {}", request.getRequestURI());
            return true;
        } catch (Exception e) {
            log.error("JWT验证过程异常: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"msg\": \"Unauthorized: Token validation error\", \"data\": null}");
            return false;
        }
    }

    /**
     * 请求处理后执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 清理线程本地变量
        BaseContext.removeCurrentId();
        BaseContext.removeCurrentIdentity();
    }

    /**
     * 请求处理完成后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null) {
            logger.error("请求处理异常: {}", request.getRequestURI(), ex);
        }
    }

    /**
     * 安全地将Object转换为Long类型
     */
    private Long convertToLong(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
