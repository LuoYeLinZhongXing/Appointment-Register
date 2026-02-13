package com.luoye.config;

import com.luoye.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * jwt拦截器配置类
 */
@Configuration
public class JwtInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加JWT拦截器，指定需要验证的路径
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/patient/**") // 患者相关路径
                .addPathPatterns("/doctor/**")  // 医生相关路径
                .addPathPatterns("/admin/**")   // 管理员相关路径
                .addPathPatterns("/slot/**")   // 号源相关路径
                .addPathPatterns("/dept/**")    // 科室相关路径
                .addPathPatterns("/order/**")   // 订单相关路径
                // 完整排除所有Swagger/Knife4j相关路径
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/doc.html")
                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/swagger-resources/**")
                .excludePathPatterns("/v2/api-docs")
                .excludePathPatterns("/v3/api-docs")
                .excludePathPatterns("/swagger-config")
                .excludePathPatterns("/swagger-ui-bundle.js")
                .excludePathPatterns("/swagger-ui-standalone-preset.js")
                // 原有的排除路径
                .excludePathPatterns("/patient/register")
                .excludePathPatterns("/patient/login")
                .excludePathPatterns("/doctor/register")
                .excludePathPatterns("/doctor/login")
                .excludePathPatterns("/admin/register")
                .excludePathPatterns("/admin/login");
    }
}
