package com.luoye.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    // 缓存键前缀
    private static final String CACHE_PREFIX_ADMIN = "admin:";
    private static final String CACHE_PREFIX_DOCTOR = "doctor:";
    private static final String CACHE_PREFIX_DEPT = "dept:";
    // 缓存过期时间（分钟）
    private static final long CACHE_EXPIRE_TIME = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;


    // 删除列表中指定元素的Lua脚本
    private static final String REMOVE_LIST_ELEMENT_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local targetId = ARGV[1]\n" +
                    "local items = redis.call('LRANGE', key, 0, -1)\n" +
                    "redis.call('DEL', key)\n" +
                    "for i, item in ipairs(items) do\n" +
                    "    local queue = cjson.decode(item)\n" +
                    "    if tostring(queue.id) ~= targetId then\n" +
                    "        redis.call('RPUSH', key, item)\n" +
                    "    end\n" +
                    "end\n" +
                    "return 1";
    // 取消号源预订的Lua脚本
    private static final String CANCEL_SLOT_SCRIPT =
            "local bookedCountKey = KEYS[1]\n" +
                    "local totalCountKey = KEYS[2]\n" +
                    "local statusKey = KEYS[3]\n" +
                    "local currentStatus = tonumber(ARGV[1])\n" +
                    "\n" +
                    "-- 获取当前预订数\n" +
                    "local currentBooked = tonumber(redis.call('GET', bookedCountKey)) or 0\n" +
                    "\n" +
                    "-- 检查是否有可取消的预订\n" +
                    "if currentBooked <= 0 then\n" +
                    "    return 0  -- 没有可取消的预订\n" +
                    "end\n" +
                    "\n" +
                    "-- 原子性减少预订数\n" +
                    "local newBooked = redis.call('DECR', bookedCountKey)\n" +
                    "\n" +
                    "-- 如果之前是已约满状态，现在恢复为可用状态\n" +
                    "if currentStatus == 2 and newBooked >= 0 then\n" +
                    "    redis.call('SET', statusKey, 1)  -- 1表示可用状态\n" +
                    "end\n" +
                    "\n" +
                    "return 1  -- 操作成功";
    /**
     * 检查Redis连接是否正常
     * @return 连接状态
     */
    public boolean isConnected() {
        try {
            // 尝试执行简单的ping命令来检测连接
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            return result != null && "PONG".equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Redis连接状态信息
     * @return 连接状态描述
     */
    public String getConnectionStatus() {
        try {
            if (isConnected()) {
                return "Redis连接正常";
            } else {
                return "Redis连接失败";
            }
        } catch (Exception e) {
            return "Redis连接状态未知: " + e.getMessage();
        }
    }

    /**
     * 普通数据存入缓存
     * @param key 键
     * @param value 值
     * @return 存储结果
     */
    public boolean set(String key, Object value) {
        try {
            if (key == null || value == null) {
                return false;
            }
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置存活时间的数据存入缓存
     * @param key 键
     * @param value 值
     * @param time 存活时间
     * @param timeUnit 时间单位
     * @return 存储结果
     */
    public boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (key == null || value == null) {
                return false;
            }

            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                return set(key, value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 带默认值的获取缓存数据方法
     * @param key 键
     * @param defaultValue 默认值
     * @return 缓存数据或默认值
     * @param <T> 泛型
     */
    public <T> T get(String key, T defaultValue) {
        T result = get(key);
        return result != null ? result : defaultValue;
    }

    /**
     * 获取缓存数据
     * @param key 键
     * @return 缓存数据
     * @param <T> 泛型
     */
    public <T> T get(String key) {
        try {
            if (key == null) {
                return null;
            }
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 类型安全的获取缓存数据方法
     * @param key 键
     * @param clazz 目标类型
     * @return 缓存数据
     * @param <T> 泛型
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            if (key == null || clazz == null) {
                return null;
            }

            Object obj = redisTemplate.opsForValue().get(key);
            if (obj == null) {
                return null;
            }

            // 如果已经是目标类型，直接返回
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }

            // 使用ObjectMapper进行类型转换
            return objectMapper.convertValue(obj, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 类型安全的获取List缓存数据方法
     * @param key 键
     * @param elementType 元素类型
     * @return 缓存数据列表
     * @param <T> 泛型
     */
    public <T> List<T> getList(String key, Class<T> elementType) {
        try {
            if (key == null || elementType == null) {
                return null;
            }

            Object obj = redisTemplate.opsForValue().get(key);
            if (obj == null) {
                return null;
            }

            // 如果已经是List且元素类型匹配，直接返回
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (list.isEmpty()) {
                    return (List<T>) list;
                }

                Object firstElement = list.get(0);
                if (elementType.isInstance(firstElement)) {
                    return (List<T>) list;
                }
            }

            // 使用ObjectMapper进行集合类型转换
            return objectMapper.convertValue(obj,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量获取缓存数据
     * @param keys 键列表
     * @return 缓存数据列表
     */
    public List<Object> multiGet(List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 批量设置缓存数据
     * @param map 键值对映射
     * @return 操作结果
     */
    public boolean multiSet(java.util.Map<String, Object> map) {
        try {
            if (map == null || map.isEmpty()) {
                return false;
            }
            redisTemplate.opsForValue().multiSet(map);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 带过期时间的批量设置
     * @param map 键值对映射
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 操作结果
     */
    public boolean multiSetWithExpire(java.util.Map<String, Object> map, long timeout, TimeUnit unit) {
        try {
            if (map == null || map.isEmpty()) {
                return false;
            }

            redisTemplate.opsForValue().multiSet(map);
            map.keySet().forEach(key -> redisTemplate.expire(key, timeout, unit));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除缓存数据
     * @param key 键
     * @return 删除结果
     */
    public boolean remove(String key) {
        try {
            if (key == null) {
                return false;
            }
            Boolean delete = redisTemplate.delete(key);
            return delete != null && delete;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量删除缓存数据
     * @param keys 键列表
     * @return 删除的键数量
     */
    public long removeBatch(List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return 0L;
            }
            Long deleted = redisTemplate.delete(keys);
            return deleted != null ? deleted : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 模糊删除（根据模式删除）
     * @param pattern 模式
     * @return 删除的键数量
     */
    public long removeByPattern(String pattern) {
        try {
            if (pattern == null) {
                return 0L;
            }

            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0L;
            }

            Long deleted = redisTemplate.delete(keys);
            return deleted != null ? deleted : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 判断缓存中是否存在该键
     * @param key 键
     * @return 存在结果
     */
    public boolean hasKey(String key) {
        try {
            if (key == null) {
                return false;
            }
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 自增操作 - 使用Redisson保证原子性
     * @param key 键
     * @param delta 增量
     * @return 增加后的值
     */
    public Long increment(String key, long delta) {
        try {
            if (key == null) {
                return null;
            }

            // 使用Redisson的原子性操作
            RLock lock = redissonClient.getLock("atomic_op_lock:" + key);

            // 获取锁，等待1秒，持有3秒
            if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                try {
                    // 执行原子性增操作
                    return redisTemplate.opsForValue().increment(key, delta);
                } finally {
                    lock.unlock();
                }
            }
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 自减操作 - 使用Redisson保证原子性
     * @param key 键
     * @param delta 减量
     * @return 减少后的值
     */
    public Long decrement(String key, long delta) {
        try {
            if (key == null) {
                return null;
            }

            // 使用Redisson的原子性操作
            RLock lock = redissonClient.getLock("atomic_op_lock:" + key);

            // 获取锁，等待1秒，持有3秒
            if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                try {
                    // 执行原子性减操作
                    return redisTemplate.opsForValue().increment(key, -delta);
                } finally {
                    lock.unlock();
                }
            }
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 直接使用RedisTemplate进行自增操作（无锁版本）
     * @param key 键
     * @return 增加后的值
     */
    public Long incrementDirect(String key) {
        try {
            if (key == null) {
                return null;
            }
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            System.err.println("Redis直接自增操作失败，key: " + key + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 直接使用RedisTemplate进行自减操作
     * @param key 键
     * @return 减少后的值
     */
    public Long decrementDirect(String key) {
        try {
            if (key == null) {
                return null;
            }
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            System.err.println("Redis直接自减操作失败，key: " + key + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将元素添加到列表头部（Left Push）- 带分布式锁保护
     * @param key 列表键
     * @param value 要添加的值
     * @return 操作结果
     */
    public boolean lpush(String key, Object value) {
        if (key == null || value == null) {
            return false;
        }


        try {
            String doctorId = null;

            if (key.contains(":")) {
                String[] parts = key.split(":");
                for (int i = 0; i < parts.length; i++) {
                    if ("doctor".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                        doctorId = parts[i + 1];
                        break;
                    }
                }
            }
            // 对于队列操作，使用医生ID作为锁的粒度
            String lockKey = "lock:queue:" + doctorId;

            if (tryLock(lockKey, 1, 3, TimeUnit.SECONDS)) {
                try {
                    redisTemplate.opsForList().leftPush(key, value);
                    return true;
                } finally {
                    unlock(lockKey);
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将多个元素添加到列表头部
     * @param key 列表键
     * @param values 要添加的值列表
     * @return 操作结果
     */
    public boolean lpushAll(String key, List<Object> values) {
        try {
            if (key == null || values == null || values.isEmpty()) {
                return false;
            }
            redisTemplate.opsForList().leftPushAll(key, values);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将元素添加到列表尾部
     * @param key 列表键
     * @param value 要添加的值
     * @return 操作结果
     */
    public boolean rpush(String key, Object value) {
        // 参数校验
        if (key == null || value == null) {
            return false;
        }

        String lockKey = "lock:list_push:" + key;

        try {
            // 获取分布式锁
            boolean locked = tryLock(lockKey, 3, 10, TimeUnit.SECONDS);
            if (!locked) {
                System.err.println("获取列表推送锁超时，key: " + key);
                return false;
            }

            try {
                // 执行Redis操作
                redisTemplate.opsForList().rightPush(key, value);
                return true;
            } catch (Exception redisEx) {
                System.err.println("Redis rpush操作失败，key: " + key + ", error: " + redisEx.getMessage());
                return false;
            } finally {
                // 释放锁
                unlock(lockKey);
            }

        } catch (Exception e) {
            System.err.println("获取分布式锁或执行操作时发生异常，key: " + key + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置键的过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 操作结果
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            if (key == null) {
                return false;
            }
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取列表中的所有元素
     * @param key 列表键
     * @return 列表元素
     */
    public List<Object> lrange(String key, long start, long end) {
        try {
            if (key == null) {
                return new ArrayList<>();
            }
            List<Object> result = redisTemplate.opsForList().range(key, start, end);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取列表长度
     * @param key 列表键
     * @return 列表长度
     */
    public Long llen(String key) {
        try {
            if (key == null) {
                return 0L;
            }
            Long length = redisTemplate.opsForList().size(key);
            return length != null ? length : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 弹出列表头部元素
     * @param key 列表键
     * @return 弹出的元素
     */
    public Object lpop(String key) {
        try {
            if (key == null) {
                return null;
            }
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 弹出列表尾部元素
     * @param key 列表键
     * @return 弹出的元素
     */
    public Object rpop(String key) {
        try {
            if (key == null) {
                return null;
            }
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行指定的Lua脚本
     * @param scriptText Lua脚本内容
     * @param keys 键列表
     * @param args 参数列表
     * @param returnType 返回类型
     * @param <T> 返回类型泛型
     * @return 脚本执行结果
     */
    public <T> T executeLuaScript(String scriptText, List<String> keys, List<String> args, Class<T> returnType) {
        try {
            if (scriptText == null || scriptText.isEmpty()) {
                throw new IllegalArgumentException("Lua脚本内容不能为空");
            }

            if (returnType == null) {
                throw new IllegalArgumentException("返回类型不能为空");
            }

            DefaultRedisScript<T> script = new DefaultRedisScript<>();
            script.setScriptText(scriptText);
            script.setResultType(returnType);

            // 确保所有参数都是字符串类型，避免Redis Lua脚本参数类型错误
            List<String> processedArgs = new ArrayList<>();
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null) {
                        // 将所有参数转换为字符串，确保Redis能正确处理
                        processedArgs.add(String.valueOf(arg));
                    } else {
                        processedArgs.add("");
                    }
                }
            }


            // 使用处理后的参数数组，而不是原始的args数组
            String[] argsArray = processedArgs.toArray(new String[0]);

            return redisTemplate.execute(script, keys, (Object[]) argsArray);

        } catch (Exception e) {
            System.err.println("执行Lua脚本失败: " + e.getMessage());
            // 打印详细的错误信息用于调试
            System.err.println("脚本内容: " + scriptText);
            System.err.println("键列表: " + keys);
            System.err.println("参数列表: " + args);
            throw new RuntimeException("Lua脚本执行失败", e);
        }
    }

    /**
     * 执行指定的Lua脚本（简化版）
     * @param scriptText Lua脚本内容
     * @param key 单个键
     * @param args 参数列表
     * @param returnType 返回类型
     * @param <T> 返回类型泛型
     * @return 脚本执行结果
     */
    public <T> T executeLuaScript(String scriptText, String key, List<String> args, Class<T> returnType) {
        return executeLuaScript(scriptText, Collections.singletonList(key), args, returnType);
    }

    /**
     * Redisson分布式锁 - 获取锁
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.tryLock(waitTime, leaseTime, unit);
        }catch (Exception e){
            return false;
        }
    }

    /**
     * Redisson分布式锁 - 释放锁
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            // 先检查锁是否存在
            if (lock.isLocked()) {
                // 如果是当前线程持有，则释放
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    return true;
                } else {
                    return false;
                }
            } else {
                return true; // 锁已不存在，视为释放成功
            }
        } catch (Exception e) {
            // 尝试强制释放（防止死锁）
            try {
                RLock lock = redissonClient.getLock(lockKey);
                lock.forceUnlock();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * 从列表中删除指定ID的元素
     * @param key 列表键
     * @param elementId 要删除的元素ID
     * @return 操作结果
     */
    public boolean removeListElementById(String key, String elementId) {
        try {
            if (key == null || elementId == null) {
                return false;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(REMOVE_LIST_ELEMENT_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(script, Collections.singletonList(key), elementId);
            return result != null && result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 原子性扣减号源库存
     * @param slotId 号源ID
     * @param totalCount 总号源数
     * @return 是否扣减成功（1成功，0失败）
     */
    public Integer decrementSlotStock(Long slotId, Integer totalCount) {
        try {
            if (slotId == null || totalCount == null || totalCount <= 0) {
                return 0;
            }

            //设置键
            String bookedCountKey = "slot:" + slotId + ":bookedCount";
            String totalCountKey = "slot:" + slotId + ":totalCount";

            // 先设置总号源数（如果不存在的话）
            if (!hasKey(totalCountKey)) {
                set(totalCountKey, totalCount);
            }

            // 获取当前已预订数量
            Integer currentBooked = getSlotBookedCount(slotId);
            Integer currentTotal = getSlotTotalCount(slotId);

            if(currentBooked <currentTotal){
                increment(bookedCountKey,1);
                return 1;
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取号源已预订数量
     * @param slotId 号源ID
     * @return 已预订数量
     */
    public Integer getSlotBookedCount(Long slotId) {
        try {
            if (slotId == null) {
                return 0;
            }

            String bookedCountKey = "slot:" + slotId + ":bookedCount";
            Integer bookedCount = get(bookedCountKey, Integer.class);
            return bookedCount != null ? bookedCount : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取号源总数量
     * @param slotId 号源ID
     * @return 总数量
     */
    public Integer getSlotTotalCount(Long slotId) {
        try {
            if (slotId == null) {
                return 0;
            }

            String totalCountKey = "slot:" + slotId + ":totalCount";
            Integer totalCount = get(totalCountKey, Integer.class);
            return totalCount != null ? totalCount : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 通用的实体缓存获取方法
     * @param prefix 缓存键前缀
     * @param id 实体ID
     * @param clazz 实体类型
     * @param queryFunction 数据库查询函数
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param <T> 实体类型
     * @return 实体对象
     */
    public <T> T getEntityWithCache(String prefix, Long id, Class<T> clazz,
                                    java.util.function.Function<Long, T> queryFunction,
                                    long expireTime, TimeUnit timeUnit) {
        if (id == null || prefix == null || clazz == null || queryFunction == null) {
            return null;
        }

        String cacheKey = prefix + id;
        String nullKey = cacheKey + ":null";

        // 先从Redis缓存查询
        T entity = get(cacheKey, clazz);
        if (entity != null) {
            return entity;
        }

        // 缓存中没有，执行查询函数查询数据库
        entity = queryFunction.apply(id);
        if (entity != null) {
            // 将查询结果存入Redis缓存
            set(cacheKey, entity, expireTime, timeUnit);
        } else {
            // 数据库中也不存在，缓存空值防止穿透（短时间过期）
            set(nullKey, "NULL", 5, TimeUnit.MINUTES);
        }

        return entity;
    }

    /**
     * 通用的实体缓存获取方法
     * @param prefix 缓存键前缀
     * @param id 实体ID
     * @param clazz 实体类型
     * @param queryFunction 数据库查询函数
     * @param <T> 实体类型
     * @return 实体对象
     */
    public <T> T getEntityWithCache(String prefix, Long id, Class<T> clazz,
                                    java.util.function.Function<Long, T> queryFunction) {
        return getEntityWithCache(prefix, id, clazz, queryFunction, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 执行取消号源预订的Lua脚本
     * @param bookedCountKey 已预订数键
     * @param totalCountKey 总数键
     * @param statusKey 状态键
     * @param currentStatus 当前状态
     * @return 执行结果（1成功，0失败）
     */
    public Long executeSlotCancelScript(String bookedCountKey, String totalCountKey, String statusKey, Integer currentStatus) {
        try {
            if (bookedCountKey == null || totalCountKey == null || statusKey == null || currentStatus == null) {
                return 0L;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(CANCEL_SLOT_SCRIPT);
            script.setResultType(Long.class);

            List<String> keys = Arrays.asList(bookedCountKey, totalCountKey, statusKey);
            List<String> args = Arrays.asList(currentStatus.toString());

            return redisTemplate.execute(script, keys, args.toArray());
        } catch (Exception e) {
            System.err.println("执行取消号源预订Lua脚本失败: " + e.getMessage());
            return 0L;
        }
    }

    public void delete(String testKey) {

        redisTemplate.delete(testKey);
    }

    /**
     * 设置列表中指定索引位置的元素
     * @param key 列表键
     * @param index 索引位置
     * @param value 要设置的值
     * @return 操作结果
     */
    public boolean lset(String key, long index, Object value) {
        try {
            if (key == null || value == null) {
                return false;
            }
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            System.err.println("Redis lset操作失败，key: " + key + ", index: " + index + ", error: " + e.getMessage());
            return false;
        }
    }

}
