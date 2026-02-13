package com.luoye.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;

/**
 * Redis配置类
 */
@Configuration
@EnableCaching//启用缓存
public class RedisConfig {


    /**
     * 创建RedisCacheManager对象
     * @param factory Redis连接工厂
     * @param  redisObjectMapper 专门用于Redis的ObjectMapper
     * @return RedisCacheManager对象
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        // 使用配置好的ObjectMapper创建序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))//过期时间为2小时
                .disableCachingNullValues()//禁用缓存空值
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))//设置key的序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));//设置value的序列化方式

        //特殊缓存配置
        HashMap<String, RedisCacheConfiguration> configMap = new HashMap<>();

        //管理员缓存配置
        configMap.put("admin",defaultConfig.entryTtl(Duration.ofHours(2)));//缓存时间为2小时

        //科室缓存配置
        configMap.put("dept", defaultConfig.entryTtl(Duration.ofHours(2))); //缓存时间为2小时
        configMap.put("dept_list",defaultConfig.entryTtl(Duration.ofHours(2)));//缓存时间为2小时

        //患者缓存配置
        configMap.put("patient", defaultConfig.entryTtl(Duration.ofHours(2))); // 患者信息缓存6小时

        //医生缓存配置
        configMap.put("doctor",defaultConfig.entryTtl(Duration.ofHours(2)));//缓存时间为2小时
        configMap.put("doctor_list",defaultConfig.entryTtl(Duration.ofHours(2)));//缓存两小时

        //号源缓存配置
        configMap.put("slot",defaultConfig.entryTtl(Duration.ofMinutes(30)));//单个号源缓存30分钟
        configMap.put("slot_doctor_date",defaultConfig.entryTtl(Duration.ofHours(2)));//医生每日号源缓存2小时
        configMap.put("slot_inventory",defaultConfig.entryTtl(Duration.ofMinutes(10)));//号源库存缓存10分钟

        //排队信息
        configMap.put("queue_doctor",defaultConfig.entryTtl(Duration.ofMinutes(10)));//缓存时间为10分钟

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)//设置默认缓存配置
                .withInitialCacheConfigurations(configMap)//设置特殊缓存配置
                .transactionAware()//添加事务感知功能
                .build();//创建RedisCacheManager对象并返回
    }

    /**
     * 创建RedisTemplate对象
     * @param factory Redis连接工厂
     * @param objectMapper ObjectMapper对象
     * @return RedisTemplate对象
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用配置好的ObjectMapper
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
