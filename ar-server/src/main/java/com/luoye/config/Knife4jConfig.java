package com.luoye.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        // 配置服务器信息
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("http://localhost:8080").description("本地开发环境"));

        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("预约挂号系统API文档")
                        .description("基于Spring Boot和Knife4j的医疗预约挂号系统API接口文档")
                        .version("1.0")
                        .contact(new Contact()
                                .name("落叶林中行")
                                .email("anlinwei_2022@qq.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers);
    }

    // 全部接口分组
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("全部接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/**")
                .build();
    }

    // 管理员接口分组
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("管理员接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/admin/**")
                .build();
    }

    // 医生接口分组
    @Bean
    public GroupedOpenApi doctorApi() {
        return GroupedOpenApi.builder()
                .group("doctor")
                .displayName("医生接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/doctor/**")
                .build();
    }

    // 患者接口分组
    @Bean
    public GroupedOpenApi patientApi() {
        return GroupedOpenApi.builder()
                .group("patient")
                .displayName("患者接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/patient/**")
                .build();
    }

    // 科室接口分组
    @Bean
    public GroupedOpenApi deptApi() {
        return GroupedOpenApi.builder()
                .group("dept")
                .displayName("科室接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/dept/**")
                .build();
    }

    // 号源接口分组
    @Bean
    public GroupedOpenApi slotApi() {
        return GroupedOpenApi.builder()
                .group("slot")
                .displayName("号源接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/slot/**")
                .build();
    }

    // 订单接口分组
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("order")
                .displayName("订单接口")
                .packagesToScan("com.luoye.controller")
                .pathsToMatch("/order/**")
                .build();
    }
}
