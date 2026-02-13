package com.luoye.controller;

import com.luoye.Result;
import com.luoye.entity.Admin;
import com.luoye.service.AdminService;
import com.luoye.util.RedisUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
@Tag(name = "测试接口")
public class TestController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AdminService adminService;

    @GetMapping("/cache-health")
    public Result<Map<String, Object>> checkCacheHealth() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 测试基本Redis连接
            boolean isConnected = redisUtil.isConnected();
            result.put("redisConnected", isConnected);

            // 测试缓存读写
            String testKey = "health_test_" + System.currentTimeMillis();
            String testValue = "test_value";

            redisUtil.set(testKey, testValue, 1, TimeUnit.MINUTES);
            String retrievedValue = redisUtil.get(testKey, String.class);
            redisUtil.delete(testKey);

            result.put("cacheReadWrite", testValue.equals(retrievedValue));

            // 测试实体缓存
            Admin admin = adminService.getById(1L);
            result.put("entityCache", admin != null);

            result.put("status", "healthy");
            return Result.success(result);

        } catch (Exception e) {
            result.put("status", "unhealthy");
            result.put("error", e.getMessage());
            return Result.error("缓存健康检查失败: " + e.getMessage());
        }
    }
}
