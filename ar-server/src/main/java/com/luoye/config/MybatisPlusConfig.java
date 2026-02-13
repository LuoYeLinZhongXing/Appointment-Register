package com.luoye.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;  // 添加正确的导入
import java.sql.Connection;
import java.sql.SQLException;

/**
 * MyBatis Plus分页插件配置
 */
@Configuration
public class MybatisPlusConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        // 预热数据库连接池
        try {
            Connection conn = dataSource.getConnection();
            conn.close(); // 归还连接到池中
        } catch (SQLException e) {
            System.err.println("数据库连接池预热失败: " + e.getMessage());
        }
    }

    /**
     * MyBatis Plus分页插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
