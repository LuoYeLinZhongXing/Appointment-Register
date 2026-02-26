package com.luoye;


import org.springframework.boot.SpringApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.luoye")
@EnableAspectJAutoProxy
@MapperScan("com.luoye.mapper")
@EnableScheduling
@EnableAsync  // 启用异步支持
public class ArApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArApplication.class, args);
    }

}
