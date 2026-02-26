package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoye.context.BaseContext;
import com.luoye.dto.doctor.DoctorCallDTO;
import com.luoye.entity.Doctor;
import com.luoye.entity.Order;
import com.luoye.entity.Patient;
import com.luoye.entity.Queue;
import com.luoye.mapper.DoctorMapper;
import com.luoye.mapper.OrderMapper;
import com.luoye.mapper.PatientMapper;
import com.luoye.mapper.QueueMapper;
import com.luoye.service.DoctorService;
import com.luoye.service.QueueService;
import com.luoye.util.RedisUtil;
import com.luoye.constant.MessageConstant;
import com.luoye.vo.QueueDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueueServiceImpl extends ServiceImpl<QueueMapper, Queue> implements QueueService {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private QueueMapper queueMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    @Qualifier("redisObjectMapper")
    private ObjectMapper objectMapper;

    // 缓存过期时间（小时）
    private static final int CACHE_EXPIRE_HOURS = 24;
    @Autowired
    private OrderMapper orderMapper;


    /**
     * 初始化医生队列
     *
     * @param doctorId 医生ID
     * @return 是否初始化成功
     */
    @Override
    public boolean initializeDoctorQueueInRedis(Long doctorId) {
        try {
            // 检查医生是否存在且在职
            Doctor doctor = doctorService.getById(doctorId);
            if (doctor == null || doctor.getStatus() != 1) {
                return false;
            }

            String cacheKey = "queue_doctor::" + doctorId;

            redisUtil.remove(cacheKey);

            // 从数据库查询该医生的所有排队记录
            QueryWrapper<Queue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("doctor_id", doctorId)
                    .in("queue_status",
                            Queue.QUEUE_STATUS_WAITING,    // 1 - 等待中
                            Queue.QUEUE_STATUS_CALLING,    // 2 - 呼叫中
                            Queue.QUEUE_STATUS_TREATING)   // 3 - 就诊中 ← 添加这个状态
                    .orderByAsc("queue_number");


            List<Queue> list = this.list(queryWrapper);

            if (list.isEmpty()) {
                list = new ArrayList<>();
            }
            // 将队列数据存入Redis List
            for (Queue queue : list) {
                String jsonStr = objectMapper.writeValueAsString(queue);
                if (queue.getIsPriority() == 1) {
                    // 急诊插入到List头部
                    redisUtil.lpush(cacheKey, jsonStr);
                } else {
                    // 普通插入到List尾部
                    redisUtil.rpush(cacheKey, jsonStr);
                }
            }
            //设置缓存过期时间
            redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            return true;


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 批量初始化所有在职医生的队列
     *
     * @return 初始化成功的医生数量
     */
    @Override
    public int initializeAllDoctorsQueue() {
        long count;

        try {
            // 查询所有在职医生
            QueryWrapper<Doctor> doctorQuery = new QueryWrapper<>();
            doctorQuery.eq("status", 1); // 1表示在职

            List<Doctor> doctors = doctorMapper.selectList(doctorQuery);

            // 为每个医生初始化队列
            count = doctors.parallelStream()
                    .filter(doctor -> initializeDoctorQueueInRedis(doctor.getId()))
                    .count();
            return (int) count;
        } catch (Exception e) {
            log.error("批量初始化医生队列失败", e);
            return 0;
        }


    }

    /**
     * 从Redis获取医生队列 - 从List中获取
     * @param doctorId 医生ID
     * @return 医生队列
     */
    @Override
    public List<Queue> getDoctorQueueFromRedis(Long doctorId) {
        String cacheKey = "queue_doctor::" + doctorId;
        List<Queue> queueList = new ArrayList<>();

        try {
            // 检查缓存是否存在
            if (!redisUtil.hasKey(cacheKey)) {
                // 缓存不存在，就查询数据库，并且初始化队列

                initializeDoctorQueueInRedis(doctorId);
                // 重新获取初始化后的缓存数据
                if (redisUtil.hasKey(cacheKey)) {
                    List<Object> objectList = redisUtil.lrange(cacheKey, 0, -1);
                    if (objectList != null && !objectList.isEmpty()) {
                        for (Object obj : objectList) {
                            try {
                                String jsonStr = obj.toString();
                                Queue queue = objectMapper.readValue(jsonStr, Queue.class);
                                queueList.add(queue);
                            } catch (JsonProcessingException e) {
                                log.error("解析队列数据失败: " + obj, e);
                            }
                        }
                    }
                }
                return queueList;
            }

            // 从Redis List获取所有元素
            List<Object> objectList = redisUtil.lrange(cacheKey, 0, -1);
            if (objectList != null && !objectList.isEmpty()) {
                for (Object obj : objectList) {
                    try {
                        // 将Object转换为String再解析
                        String jsonStr = obj.toString();
                        Queue queue = objectMapper.readValue(jsonStr, Queue.class);
                        queueList.add(queue);
                    } catch (JsonProcessingException e) {
                        log.error("解析队列数据失败: " + obj, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取医生队列缓存失败，医生ID:" + doctorId, e);
        }


        System.out.println(queueList);
        return queueList;
    }

    /**
     * 从Redis获取医生队列详情（包装后的数据）
     * @param doctorId 医生ID
     * @return 队列详情列表
     */
    @Override
    public List<QueueDetailVO> getDoctorQueueDetailsFromRedis(Long doctorId) {
        String cacheKey = "queue_doctor::" + doctorId;
        List<QueueDetailVO> detailList = new ArrayList<>();

        try {
            log.info("开始获取医生队列详情，医生ID: {}, 缓存键: {}", doctorId, cacheKey);
            
            // 检查缓存是否存在
            boolean hasKey = redisUtil.hasKey(cacheKey);
            log.info("缓存是否存在: {}", hasKey);
            
            if (!hasKey) {
                // 缓存不存在，初始化队列
                log.info("缓存不存在，开始初始化医生队列，医生ID: {}", doctorId);
                boolean initSuccess = initializeDoctorQueueInRedis(doctorId);
                log.info("初始化结果: {}", initSuccess);
                
                // 检查初始化后缓存是否存在
                hasKey = redisUtil.hasKey(cacheKey);
                log.info("初始化后缓存是否存在: {}", hasKey);
            }

            if (hasKey) {
                // 直接从Redis获取并转换，避免调用getDoctorQueueFromRedis造成的重复处理
                List<Object> objectList = redisUtil.lrange(cacheKey, 0, -1);
                log.info("从Redis获取到的对象列表大小: {}", objectList != null ? objectList.size() : 0);
                
                if (objectList != null && !objectList.isEmpty()) {
                    log.info("开始处理Redis中的队列数据...");
                    for (int i = 0; i < objectList.size(); i++) {
                        Object obj = objectList.get(i);
                        try {
                            String jsonStr = obj.toString();
                            log.debug("处理第{}个队列元素: {}", i, jsonStr);
                            
                            Queue queue = objectMapper.readValue(jsonStr, Queue.class);
                            log.info("解析队列对象成功，队列ID: {}, 患者ID: {}, 状态: {}", 
                                   queue.getId(), queue.getPatientId(), queue.getQueueStatus());

                            QueueDetailVO detailVO = new QueueDetailVO();

                            // 设置队列ID
                            detailVO.setId(queue.getId());

                            // 获取订单信息
                            Order order = redisUtil.getEntityWithCache("order::", queue.getOrderId(), Order.class,
                                    orderId -> orderMapper.selectById(orderId));
                            if (order != null) {
                                detailVO.setOrderNo(order.getOrderNo());
                                log.debug("获取订单信息成功，订单号: {}", order.getOrderNo());
                            }

                            // 获取患者信息
                            com.luoye.entity.Patient patient = redisUtil.getEntityWithCache("patient::", queue.getPatientId(), com.luoye.entity.Patient.class,
                                    patientId -> patientMapper.selectById(patientId));
                            if (patient != null) {
                                detailVO.setPatientName(patient.getName());
                                log.debug("获取患者信息成功，患者姓名: {}", patient.getName());
                            }

                            // 设置其他字段（原样返回）
                            detailVO.setIsPriority(queue.getIsPriority());
                            detailVO.setQueueNumber(queue.getQueueNumber());
                            detailVO.setQueueStatus(queue.getQueueStatus());
                            detailVO.setCheckInTime(queue.getCheckInTime());
                            detailVO.setCallTime(queue.getCallTime());
                            detailVO.setStartTime(queue.getStartTime());
                            detailVO.setEndTime(queue.getEndTime());
                            detailVO.setMissedCount(queue.getMissedCount());
                            detailVO.setMaxMissedAllowed(queue.getMaxMissedAllowed());

                            detailList.add(detailVO);
                            log.info("添加队列详情到结果列表，当前列表大小: {}", detailList.size());
                            
                        } catch (JsonProcessingException e) {
                            log.error("解析队列数据失败: " + obj, e);
                        }
                    }
                } else {
                    log.warn("Redis中没有找到队列数据");
                }
            } else {
                log.warn("缓存初始化后仍然不存在");
            }

        } catch (Exception e) {
            log.error("获取医生队列详情失败，医生ID:" + doctorId, e);
        }

        log.info("最终返回的队列详情列表大小: {}", detailList.size());
        return detailList;
    }


    /**
     * 将队列数据同步到Redis
     *
     * @param doctorId  医生ID
     * @param queueList 队列列表
     * @return 是否同步成功
     */
    public boolean syncQueueToRedis(Long doctorId, List<Queue> queueList) {

        String cacheKey = "queue_doctor::" + doctorId;
        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(queueList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }

        // 缓存数据
        return redisUtil.set(cacheKey, jsonStr, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

    }

    /**
     * 订单支付成功后的处理
     *
     * @param order 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleQueueAfterPayment(Order order) {
        try {
            // 创建排队记录
            Queue queue = new Queue();
            queue.setOrderId(order.getId());
            queue.setPatientId(order.getPatientId());
            queue.setDoctorId(order.getDoctorId());
            queue.setDeptId(order.getDeptId());
            queue.setQueueNumber(getNextQueueNumber(order.getDoctorId()));
            queue.setQueueStatus(Queue.QUEUE_STATUS_WAITING);
            queue.setCheckInTime(LocalDateTime.now()); // 这里可能需要改为取号时间
            // 根据是否急诊设置不同的初始状态
            if (Order.EMERGENCY_URGENT.equals(order.getIsEmergency())) {
                queue.setIsPriority(1); // 设置为优先级
            } else {
                queue.setIsPriority(0); // 普通优先级
            }

            queue.setMissedCount(0); // 初始过号次数为0
            queue.setMaxMissedAllowed(3); // 最大允许过号次数
            queue.setCreateTime(LocalDateTime.now());
            queue.setUpdateTime(LocalDateTime.now());

            // 保存排队记录
            queueMapper.insert(queue);

            // 增量更新Redis缓存
            incrementUpdateQueueCache(order.getDoctorId(), queue);
        } catch (Exception e) {
            // 记录错误但不中断取号流程
            log.error("排队处理失败，订单ID: " + order.getId(), e);
        }
    }

    /**
     * 增量更新Redis缓存中的队列数据
     *
     * @param doctorId 医生ID
     * @param queue    新增的排队记录
     */
    private void incrementUpdateQueueCache(Long doctorId, Queue queue) {
        String cacheKey = "queue_doctor::" + doctorId;

        try {
            String jsonStr = objectMapper.writeValueAsString(queue);

            // 由于外层已有分布式锁，这里可以直接使用普通操作
            if (queue.getIsPriority() == 1) {
                // 急诊插入到List头部
                redisUtil.lpush(cacheKey, jsonStr);
            } else {
                // 普通插入到List尾部
                redisUtil.rpush(cacheKey, jsonStr);
            }

            // 设置过期时间
            redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        } catch (JsonProcessingException e) {
            log.error("队列对象序列化失败", e);
        } catch (Exception e) {
            log.error("更新队列缓存失败", e);
            // 降级处理：重新初始化整个缓存
            initializeDoctorQueueInRedis(doctorId);
        }
    }

    /**
     * 降级处理方法
     */
    private void fallbackIncrementUpdate(Long doctorId, Queue queue) {
        String cacheKey = "queue_doctor::" + doctorId;
        try {
            String jsonStr = objectMapper.writeValueAsString(queue);
            if (queue.getIsPriority() == 1) {
                redisUtil.lpush(cacheKey, jsonStr);
            } else {
                redisUtil.rpush(cacheKey, jsonStr);
            }
            redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("降级更新队列缓存失败", e);
        }
    }

    /**
     * 从排队队列中删除指定订单的排队记录
     *
     * @param orderId 订单ID
     */
    @Override
    @Async
    public void removeFromQueue(Long orderId) {
        try {
            // 查询该订单对应的排队记录
            QueryWrapper<Queue> queueQuery = new QueryWrapper<>();
            queueQuery.eq("order_id", orderId);

            Queue queue = queueMapper.selectOne(queueQuery);
            if (queue != null) {
                Long doctorId = queue.getDoctorId();
                Long queueId = queue.getId();

                //尝试删除Redis缓存
                try {

                    incrementRemoveFromQueueCache(doctorId, queueId);
                } catch (Exception e) {
                    log.error("Redis删除失败，但继续删除数据库记录", e);
                }

                //删除数据库记录
                int deleted = queueMapper.deleteById(queueId);
                if (deleted == 0) {
                    // 删除失败，可能是因为并发操作
                    log.warn("删除数据库排队记录失败，可能已被其他操作删除，ID:"+  queueId);
                }
            }
        } catch (Exception e) {
            // 记录错误但不中断流程
            e.printStackTrace();
        }
    }

    /**
     * 删除Redis缓存中的排队记录 - 使用Lua脚本实现原子操作
     *
     * @param doctorId 医生ID
     * @param queueId  排队记录ID
     */
    private void incrementRemoveFromQueueCache(Long doctorId, Long queueId) {
        String cacheKey = "queue_doctor::" + doctorId;

        // Lua脚本：原子性地删除指定ID的队列元素
        String luaScript =
                "local cacheKey = KEYS[1]\n" +
                        "local targetQueueId = ARGV[1]\n" +
                        "local expireHours = ARGV[2]\n" +
                        "\n" +
                        "-- 获取List中所有元素\n" +
                        "local listLength = redis.call('LLEN', cacheKey)\n" +
                        "if listLength == 0 then\n" +
                        "    return 0\n" +
                        "end\n" +
                        "\n" +
                        "-- 遍历List查找目标元素\n" +
                        "for i = 0, listLength - 1 do\n" +
                        "    local item = redis.call('LINDEX', cacheKey, i)\n" +
                        "    if item then\n" +
                        "        local queue = cjson.decode(item)\n" +
                        "        if tostring(queue.id) == targetQueueId then\n" +
                        "            -- 找到目标元素，使用LTRIM删除\n" +
                        "            if i == 0 then\n" +
                        "                -- 删除第一个元素\n" +
                        "                redis.call('LPOP', cacheKey)\n" +
                        "            elseif i == listLength - 1 then\n" +
                        "                -- 删除最后一个元素\n" +
                        "                redis.call('RPOP', cacheKey)\n" +
                        "            else\n" +
                        "                -- 删除中间元素：分两部分重新组合\n" +
                        "                local beforeList = redis.call('LRANGE', cacheKey, 0, i - 1)\n" +
                        "                local afterList = redis.call('LRANGE', cacheKey, i + 1, -1)\n" +
                        "                redis.call('DEL', cacheKey)\n" +
                        "                for _, v in ipairs(beforeList) do\n" +
                        "                    redis.call('RPUSH', cacheKey, v)\n" +
                        "                end\n" +
                        "                for _, v in ipairs(afterList) do\n" +
                        "                    redis.call('RPUSH', cacheKey, v)\n" +
                        "                end\n" +
                        "            end\n" +
                        "            -- 设置过期时间（显式转换为数字）\n" +
                        "            redis.call('EXPIRE', cacheKey, tonumber(expireHours))\n" +
                        "            return 1\n" +
                        "        end\n" +
                        "    end\n" +
                        "end\n" +
                        "\n" +
                        "return 0";

        try {
            String expireSeconds = String.valueOf(CACHE_EXPIRE_HOURS * 3600);
            List<String> args = Arrays.asList(queueId.toString(), expireSeconds);

            // 执行Lua脚本
            Long result = redisUtil.executeLuaScript(luaScript, cacheKey, args, Long.class);

            if (result == null || result == 0) {
                log.warn("从Redis删除排队记录可能失败，医生ID:" + doctorId + "，队列ID:" + queueId);
            }

        } catch (Exception e) {
            log.error("从Redis删除排队记录失败，医生ID:" + doctorId + ", 队列ID: " + queueId, e);
            // 删除失败时，重新初始化缓存（兜底方案）
            initializeDoctorQueueInRedis(doctorId);
        }
    }

    /**
     * 获取下一个排队号码 - 从缓存队列中获取
     * @param doctorId 医生ID
     * @return 下一个排队号码
     */
    @Override
    public Integer getNextQueueNumber(Long doctorId) {
        String lockKey = "lock:queue_number:" + doctorId;

        try {
            // 获取分布式锁，最多等待3秒
            boolean locked = redisUtil.tryLock(lockKey, 3000, 10000, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            try {
                // 获取医生的缓存队列
                List<Queue> queueList = getDoctorQueueFromRedis(doctorId);

                if (queueList.isEmpty()) {
                    return 1; // 队列为空，从1开始
                }

                // 获取队列中最大的排队号码
                int maxQueueNumber = queueList.stream()
                        .mapToInt(Queue::getQueueNumber)
                        .max()
                        .orElse(0);

                return maxQueueNumber + 1;

            } finally {
                // 释放锁
                redisUtil.unlock(lockKey);
            }
        } catch (Exception e) {
            log.error("获取排队号失败，医生ID:" + doctorId, e);
            throw new RuntimeException("获取排队号失败");
        }
    }

    /**
     * 获取指定患者的排队位置
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return
     */
    @Override
    public Integer getPatientQueuePosition(Long patientId, Long doctorId) {
        try{
            //获取医生队列
            List<Queue> queueList = getDoctorQueueFromRedis(doctorId);

            // 查找患者的位置（只计算等待中的患者）
            int position = 1;
            for (Queue queue : queueList) {
                if (queue.getPatientId().equals(patientId) &&
                        Queue.QUEUE_STATUS_WAITING.equals(queue.getQueueStatus())) {
                    return position;
                }
                // 只计算等待中的患者
                if (Queue.QUEUE_STATUS_WAITING.equals(queue.getQueueStatus())) {
                    position++;
                }
            }

            return -1;
       }catch (Exception e) {
            log.error("获取患者排队位置失败，患者ID:" + patientId + ", 医生ID:" + doctorId, e);
            return -1;
        }
    }

    /**
     * 患者查询排队队列
     * @param patientId 患者ID
     * @return 医生队列数据
     */
    @Override
    public List<QueueDetailVO> getPatientQueueInfo(Long patientId) {
        try {
            log.info("=== 患者查询排队队列开始 ===");
            log.info("患者ID: {}", patientId);
            
            // 查询患者在等待、呼叫、就诊中状态的队列记录
            QueryWrapper<Queue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("patient_id", patientId)
                    .in("queue_status",
                            Queue.QUEUE_STATUS_WAITING,
                            Queue.QUEUE_STATUS_CALLING,
                            Queue.QUEUE_STATUS_TREATING);

            log.info("数据库查询条件: patient_id={}, queue_status in [1,2,3]", patientId);
            List<Queue> patientQueues = this.list(queryWrapper);
            log.info("数据库查询结果数量: {}", patientQueues.size());

            if (patientQueues.isEmpty()) {
                log.warn("没有找到符合条件的队列记录");
                // 为了调试，我们直接查询所有该患者的记录
                QueryWrapper<Queue> allQuery = new QueryWrapper<>();
                allQuery.eq("patient_id", patientId);
                List<Queue> allPatientQueues = this.list(allQuery);
                log.info("该患者的所有队列记录数量: {}", allPatientQueues.size());
                if (!allPatientQueues.isEmpty()) {
                    log.info("所有记录的状态分布:");
                    allPatientQueues.forEach(q -> log.info("  队列ID: {}, 状态: {}", q.getId(), q.getQueueStatus()));
                }
                return new ArrayList<>();
            }

            // 获取唯一的队列数据（理论上应该只有一条记录）
            Queue patientQueue = patientQueues.get(0);
            Long doctorId = patientQueue.getDoctorId();
            log.info("找到患者队列记录 - 队列ID: {}, 医生ID: {}, 状态: {}", 
                   patientQueue.getId(), doctorId, patientQueue.getQueueStatus());

            // 调用getDoctorQueueFromRedis方法查询医生队列
            List<QueueDetailVO> doctorQueueDetailsFromRedis = getDoctorQueueDetailsFromRedis(doctorId);
            log.info("从医生队列获取到的详情数量: {}", doctorQueueDetailsFromRedis.size());

            // 验证返回的数据中是否包含当前患者
            boolean patientFound = doctorQueueDetailsFromRedis.stream()
                    .anyMatch(vo -> {
                        // 这里需要根据实际情况判断如何匹配患者
                        // 可能需要通过订单ID或其他方式匹配
                        return true; // 临时返回true用于测试
                    });
            log.info("返回数据中是否包含当前患者: {}", patientFound);

            log.info("查询成功，返回数据: {}", doctorQueueDetailsFromRedis);
            log.info("=== 患者查询排队队列结束 ===");
            return doctorQueueDetailsFromRedis;

        } catch (Exception e) {
            log.error("患者查询排队队列失败，患者ID:" + patientId, e);
            return new ArrayList<>();
        }
    }


    /**
     * 医生叫号
     * @return 叫号结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Queue callNextPatient() {
        // 验证医生身份
        String currentUserType = BaseContext.getCurrentIdentity();
        Long currentUserId = BaseContext.getCurrentId();

        if(currentUserId == null || doctorService.getById(currentUserId) == null){
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        if (!"DOCTOR".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }


        String lockKey = "lock:queue_call:" + currentUserId;

        try {
            //获取分布式锁
            boolean locked = redisUtil.tryLock(lockKey, 3000, 10000, TimeUnit.MILLISECONDS);
            if(!locked){
                throw new RuntimeException(MessageConstant.SYSTEM_BUSY);
            }
            try{
                //获取医生队列
                List<Queue> queueList = getDoctorQueueFromRedis(currentUserId);
                System.out.println( queueList);

                if(queueList.isEmpty()){
                    throw new RuntimeException(MessageConstant.QUEUE_IS_EMPTY);
                }
                //获取第一个等待中的患者（由于队列已按优先级和号码排序，第一个就是目标患者）
                Queue targetQueue = queueList.stream()
                        .filter(q -> Queue.QUEUE_STATUS_WAITING.equals(q.getQueueStatus()))
                        .findFirst()
                        .orElse(null);
                if(targetQueue == null){
                    throw new RuntimeException(MessageConstant.NO_PATIENT_TO_CALL);
                }

                //更新排队状态为呼叫中
                targetQueue.setQueueStatus(Queue.QUEUE_STATUS_CALLING);
                targetQueue.setCallTime(LocalDateTime.now());
                targetQueue.setUpdateTime(LocalDateTime.now());

                //更新数据库
                queueMapper.updateById(targetQueue);

                //更新缓存
                updateQueueInRedis(currentUserId,targetQueue);

                return targetQueue;
            }finally {
                redisUtil.unlock(lockKey);
            }
        }catch (Exception e){
            throw new RuntimeException("叫号失败："+ e.getMessage());
        }
    }

    /**
     * 处理患者过号
     * @param queueId 队列ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleMissedPatient(Long queueId){

        Long doctorId = BaseContext.getCurrentId();
        String lockKey = "lock:queue_missed:" + doctorId;
        try{
            boolean locked = redisUtil.tryLock(lockKey, 3000, 10000, TimeUnit.MILLISECONDS);
            if(!locked){
                throw new RuntimeException(MessageConstant.SYSTEM_BUSY);
            }
            try {

                Queue queue = queueMapper.selectById(queueId);
                if(queue ==null){
                    throw new RuntimeException(MessageConstant.QUEUE_NOT_FOUND);
                }
                //验证患者状态
                if (!Queue.QUEUE_STATUS_CALLING.equals(queue.getQueueStatus())) {
                    throw new RuntimeException(MessageConstant.QUEUE_STATUS_ERROR);
                }

                //增加过号次数
                queue.setMissedCount(queue.getMissedCount() + 1);
                queue.setQueueStatus(Queue.QUEUE_STATUS_WAITING);
                queue.setUpdateTime(LocalDateTime.now());

                // 从缓存队列中获取下一个排队号码
                List<Queue> queueList = getDoctorQueueFromRedis(queue.getDoctorId());
                int maxQueueNumber = queueList.stream()
                        .mapToInt(Queue::getQueueNumber)
                        .max()
                        .orElse(0);
                queue.setQueueNumber(maxQueueNumber + 1); // 设置为最大号码+1

                //判断是否超过最大允许过号次数
                if (queue.getMissedCount() >= queue.getMaxMissedAllowed()) {
                    queue.setQueueStatus(Queue.QUEUE_STATUS_MISSED);
                    //删除队列
                    queueMapper.deleteById(queueId);
                    //删除缓存
                    String cacheKey = "queue_doctor::" + queue.getDoctorId();
                    removeQueueElementFromCache(cacheKey, queueId);
                } else {
                    //更新数据库
                    queueMapper.updateById(queue);
                    //更新redis缓存 - 过号需要特殊处理：从队头删除并插入队尾
                    handleMissedQueueInRedis(queue.getDoctorId(), queue);
                }
            }finally {
                redisUtil.unlock(lockKey);
            }
        }catch (Exception e) {
            log.error("处理过号患者失败，队列ID:" + queueId, e);
            throw new RuntimeException("处理过号失败：" + e.getMessage());

        }
    }

    /**
     * 处理过号患者的缓存更新 - 使用增量更新方式
     * @param doctorId 医生ID
     * @param missedQueue 过号的队列记录
     */
    private void handleMissedQueueInRedis(Long doctorId, Queue missedQueue) {
        String cacheKey = "queue_doctor::" + doctorId;

        try {
            // 由于外层已有分布式锁，这里可以直接操作
            String jsonStr = objectMapper.writeValueAsString(missedQueue);
            Long targetQueueId = missedQueue.getId();

            // 增量更新方式：先找到目标元素的位置
            List<Object> currentList = redisUtil.lrange(cacheKey, 0, -1);
            if (currentList == null || currentList.isEmpty()) {
                log.warn("队列为空，无法处理过号，医生ID: " + doctorId);
                return;
            }

            int targetIndex = -1;
            // 找到目标元素的索引位置
            for (int i = 0; i < currentList.size(); i++) {
                try {
                    String itemStr = currentList.get(i).toString();
                    Queue queue = objectMapper.readValue(itemStr, Queue.class);
                    if (queue.getId().equals(targetQueueId)) {
                        targetIndex = i;
                        break;
                    }
                } catch (JsonProcessingException e) {
                    log.error("解析队列元素失败: " + currentList.get(i), e);
                }
            }

            if (targetIndex == -1) {
                log.warn("未找到要过号的队列元素，ID: " + targetQueueId);
                return;
            }

            // 增量更新：将目标元素移动到队尾
            if (targetIndex < currentList.size() - 1) {
                // 如果不是已经在队尾，则需要移动
                String targetElement = currentList.get(targetIndex).toString();

                // 删除目标位置的元素
                redisUtil.lset(cacheKey, targetIndex, "__DELETED__");

                // 将后面的元素向前移动
                for (int i = targetIndex; i < currentList.size() - 1; i++) {
                    redisUtil.lset(cacheKey, i, currentList.get(i + 1).toString());
                }

                // 在队尾添加目标元素
                redisUtil.lset(cacheKey, currentList.size() - 1, jsonStr);
            } else {
                // 如果已经在队尾，直接更新状态
                redisUtil.lset(cacheKey, targetIndex, jsonStr);
            }

            // 设置过期时间
            redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("过号增量更新完成，患者ID: " + missedQueue.getPatientId() + " 已更新状态");

        } catch (Exception e) {
            log.error("处理过号患者缓存更新失败，医生ID: " + doctorId + ", 队列ID: " + missedQueue.getId(), e);
            // 最后的兜底方案：重新初始化整个缓存
            initializeDoctorQueueInRedis(doctorId);
        }
    }

    /**
     * 从缓存中移除指定的队列元素 - 增量删除方式
     *
     * @param cacheKey 缓存键
     * @param queueId  要删除的队列ID
     */
    private void removeQueueElementFromCache(String cacheKey, Long queueId) {
        try {
            //直接操作
            //手动遍历删除
            List<Object> currentList = redisUtil.lrange(cacheKey, 0, -1);
            if (currentList == null || currentList.isEmpty()) {
                return; // 队列为空，无需删除
            }

            List<String> filteredList = new ArrayList<>();
            boolean targetFound = false;

            // 遍历并过滤掉目标元素
            for (Object item : currentList) {
                try {
                    String itemStr = item.toString();
                    Queue queue = objectMapper.readValue(itemStr, Queue.class);
                    if (!queue.getId().equals(queueId)) {
                        filteredList.add(itemStr);
                    } else {
                        targetFound = true;
                    }
                } catch (JsonProcessingException e) {
                    log.error("解析队列元素失败: " + item, e);
                    // 解析失败的元素也保留，避免数据丢失
                    filteredList.add(item.toString());
                }
            }

            if (targetFound) {
                // 重建队列（增量更新）
                redisUtil.remove(cacheKey);
                redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                // 再添加元素（如果有的话）
                for (String item : filteredList) {
                    redisUtil.rpush(cacheKey, item);
                }
            } else {

                log.warn("未找到要删除的队列元素，队列ID: " + queueId);
            }

        } catch (Exception e) {
            log.error("删除队列缓存元素失败，缓存键: " + cacheKey + ", 队列ID: " + queueId, e);
            // 最后兜底：重新初始化整个队列缓存
            String[] parts = cacheKey.split("::");
            if (parts.length > 1) {
                try {
                    Long doctorId = Long.parseLong(parts[1]);
                    initializeDoctorQueueInRedis(doctorId);
                } catch (NumberFormatException ex) {
                    log.error("无法从缓存键提取医生ID: " + cacheKey);
                }
            }
        }
    }
    /**
     * 开始治疗
     * @param queueId 队列ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTreatment(Long queueId){
        Long doctorId = BaseContext.getCurrentId();
        String lockKey = "lock:queue_start_treatment:" + doctorId;
        try{
            //获取分布式锁
            boolean locked = redisUtil.tryLock(lockKey, 3000, 10000, TimeUnit.MILLISECONDS);
            if(!locked){
                throw new RuntimeException(MessageConstant.SYSTEM_BUSY);
            }

            try {
                // 重新查询确保数据是最新的
                Queue  queue = queueMapper.selectById(queueId);
                if (queue == null) {
                    throw new RuntimeException("未找到排队记录");
                }

                //验证患者状态
                if(!Queue.QUEUE_STATUS_CALLING.equals(queue.getQueueStatus())){
                    throw new RuntimeException(MessageConstant.QUEUE_STATUS_ERROR);
                }

                //更新状态为就诊中
                queue.setQueueStatus(Queue.QUEUE_STATUS_TREATING);
                queue.setStartTime(LocalDateTime.now());
                queue.setUpdateTime(LocalDateTime.now());
                //更新数据库
                queueMapper.updateById(queue);

                updateQueueInRedis(doctorId,queue);
            }finally {
                redisUtil.unlock(lockKey);
            }
        }catch (Exception e){
            log.error("开始治疗失败，队列ID:" + queueId, e);
            throw new RuntimeException("开始治疗失败：" + e.getMessage());
        }
    }

    /**
     * 结束治疗
     * @param queueId 队列ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTreatment(Long queueId) {
        Long doctorId = BaseContext.getCurrentId();
        String lockKey = "lock:queue_complete_treatment:" + doctorId;

        try{
            boolean locked = redisUtil.tryLock(lockKey, 3000, 10000, TimeUnit.MILLISECONDS);
            if(!locked){
                throw new RuntimeException(MessageConstant.SYSTEM_BUSY);
            }
            try{
                //查询队列
                Queue queue = queueMapper.selectById(queueId);
                if(queue == null){
                    throw new RuntimeException(MessageConstant.QUEUE_NOT_FOUND);
                }
                //验证患者状态
                if(!Queue.QUEUE_STATUS_TREATING.equals(queue.getQueueStatus())){
                    throw new RuntimeException(MessageConstant.QUEUE_STATUS_ERROR);
                }
                //更新状态为已完成
                queue.setQueueStatus(Queue.QUEUE_STATUS_COMPLETED);
                queue.setEndTime(LocalDateTime.now());
                queue.setUpdateTime(LocalDateTime.now());

                //更新数据库
                queueMapper.updateById(queue);

                //删除缓存
                String cacheKey = "queue_doctor::" + doctorId;
                removeQueueElementFromCache(cacheKey, queueId);
                redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS,TimeUnit.HOURS);
            }finally {
                redisUtil.unlock(lockKey);
            }
        }catch (Exception e){
            log.error("结束治疗失败，队列ID:" + queueId, e);
            throw new RuntimeException("结束治疗失败：" + e.getMessage());
        }
    }

    /**
     * 更新队列缓存 - 精确更新指定项
     * @param doctorId 医生ID
     * @param updateQueue 更新的队列
     */
    private void updateQueueInRedis(Long doctorId, Queue updateQueue){
        String cacheKey = "queue_doctor::" + doctorId;

        try {
            // 由于外层已有分布式锁，这里可以直接更新指定位置的元素
            String jsonStr = objectMapper.writeValueAsString(updateQueue);

            // 获取当前队列的所有元素
            List<Object> objectList = redisUtil.lrange(cacheKey, 0, -1);
            if (objectList == null || objectList.isEmpty()) {
                log.warn("队列为空，无法更新，医生ID: " + doctorId);
                return;
            }

            // 找到要更新的元素位置
            int targetIndex = -1;
            for (int i = 0; i < objectList.size(); i++) {
                try {
                    String itemStr = objectList.get(i).toString();
                    Queue queue = objectMapper.readValue(itemStr, Queue.class);
                    if (queue.getId().equals(updateQueue.getId())) {
                        targetIndex = i;
                        break;
                    }
                } catch (JsonProcessingException e) {
                    log.error("解析队列元素失败", e);
                }
            }

            if (targetIndex != -1) {
                // 直接更新指定位置的元素
                redisUtil.lset(cacheKey, targetIndex, jsonStr);
                // 重置过期时间
                redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            } else {
                log.warn("未找到要更新的队列元素，ID: " + updateQueue.getId());
                // 降级处理
                fallbackUpdateQueueInRedis(doctorId, updateQueue);
            }

        } catch (Exception e) {
            log.error("更新队列缓存失败：" + e.getMessage());
            fallbackUpdateQueueInRedis(doctorId, updateQueue);
        }
    }
    /**
     * 降级处理方法
     */
    private void fallbackUpdateQueueInRedis(Long doctorId, Queue updateQueue) {
        String cacheKey = "queue_doctor::" + doctorId;
        try {
            // 先删除再添加（非原子性）
            redisUtil.removeListElementById(cacheKey, updateQueue.getId().toString());

            String jsonStr = objectMapper.writeValueAsString(updateQueue);
            redisUtil.rpush(cacheKey, jsonStr);

            // 设置过期时间
            redisUtil.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("降级更新队列缓存失败", e);
            // 最后的兜底方案：重新初始化整个缓存
            initializeDoctorQueueInRedis(doctorId);
        }
    }
}
