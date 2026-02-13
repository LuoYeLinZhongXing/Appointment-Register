package com.luoye.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.context.BaseContext;
import com.luoye.dto.RegisterDTO;
import com.luoye.constant.MessageConstant;
import com.luoye.dto.order.OrderCancelDTO;
import com.luoye.service.QueueService;
import com.luoye.service.SlotService;
import com.luoye.vo.OrderDetailVO;
import com.luoye.entity.*;
import com.luoye.exception.BaseException;
import com.luoye.mapper.*;
import com.luoye.service.OrderService;
import com.luoye.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 */
@Service
@Slf4j
public class OrderServiceImpl  extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Value("${order.prefix:LYLZX}")
    private String orderPrefix;

    @Autowired
    private SlotService slotService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    @Lazy
    private OrderService orderService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueueMapper queueMapper;
    /**
     * 挂号并创建订单
     * @param registerDTO 挂号信息
     * @return 订单ID
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "slot",key="#registerDTO.slotId")
    public Long registerAndCreateOrder(RegisterDTO registerDTO) {
        // 从BaseContext获取当前患者ID
        Long patientId = BaseContext.getCurrentId();
        String identity = BaseContext.getCurrentIdentity();

        log.info("调试信息 - 当前用户ID: {}, 身份: {}", patientId, identity);

        // 添加更多调试信息
        log.info("BaseContext当前状态 - ID: {}, Identity: {}",
                BaseContext.getCurrentId(),
                BaseContext.getCurrentIdentity());

        if (patientId == null) {
            log.error("用户未登录 - BaseContext中没有用户信息");
            log.error("当前线程ID: {}", Thread.currentThread().getId());
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }

        //获取号源信息
        Slot slot = slotService.getSlotById(registerDTO.getSlotId());
        if(slot == null){
            throw new BaseException(MessageConstant.SLOT_NOT_EXIST);
        }
        //获取锁
        String lockKey = "lock:slot_operation:" + slot.getId();
        boolean locked = redisUtil.tryLock(lockKey, 3000, 15000, TimeUnit.MILLISECONDS);
        if (!locked) {
            throw new BaseException(MessageConstant.SYSTEM_BUSY);
        }
        try {
            // 直接获取号源信息进行检查（利用锁保护）
            slot = slotService.getSlotById(registerDTO.getSlotId());
            if (slot == null || !slot.getStatus().equals(Slot.STATUS_AVAILABLE)) {
                throw new BaseException(MessageConstant.SLOT_NOT_AVAILABLE);
            }

            // 直接检查库存（利用锁保护）- 使用新的缓存格式
            String bookedCountKey = "slot_inventory::bookedCount::" + registerDTO.getSlotId();
            String totalCountKey = "slot_inventory::totalCount::" + registerDTO.getSlotId();

            Integer currentBooked = redisUtil.get(bookedCountKey, Integer.class);
            Integer totalCount = redisUtil.get(totalCountKey, Integer.class);

            if (currentBooked == null) currentBooked = slot.getBookedCount();
            if (totalCount == null) totalCount = slot.getTotalCount();

            if (currentBooked >= totalCount) {
                throw new BaseException("号源已被预订完，请选择其他时间段");
            }

            // 无锁预订号源
            boolean bookingSuccess = slotService.bookSlot(registerDTO.getSlotId());
            if (!bookingSuccess) {
                throw new BaseException("号源已被预订完，请选择其他时间段");
            }

            // 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setPatientId(patientId);
            order.setSlotId(registerDTO.getSlotId());
            order.setDoctorId(slot.getDoctorId());
            order.setDeptId(slot.getDeptId());
            order.setFeeAmount(slot.getFeeAmount());
            order.setOrderStatus(Order.ORDER_STATUS_PENDING);
            order.setIsEmergency(registerDTO.getIsEmergency() != null ?
                    registerDTO.getIsEmergency() : Order.EMERGENCY_NORMAL);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            int insert = orderMapper.insert(order);
            if (insert <= 0) {
                throw new BaseException(MessageConstant.ORDER_CREATE_FAILED);
            }

            return order.getId();

        } finally {
            redisUtil.unlock(lockKey);
        }
    }


    /**
     * 根据订单ID获取订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    @Override
    @Cacheable(cacheNames = "order",key = "#orderId")//缓存订单详情
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    public OrderDetailVO getOrderDetailById(Long orderId) {
        //获取订单基本信息
        Order order = orderService.getOrderById(orderId);
        if(order == null){
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }

        //创建订单详情VO
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        BeanUtils.copyProperties(order, orderDetailVO);

        //获取关联的患者信息
        Patient patient = redisUtil.getEntityWithCache("patient::", order.getPatientId(), Patient.class,
                patientId -> patientMapper.selectById(patientId));
        if(patient != null){
            orderDetailVO.setPatientName(patient.getName());
        }

        //获取关联的医生信息
        Doctor doctor = redisUtil.getEntityWithCache("doctor::", order.getDoctorId(), Doctor.class,
                doctorId -> doctorMapper.selectById(doctorId));
        if (doctor != null) {
            orderDetailVO.setDoctorName(doctor.getName());
        }

        //获取关联的科室信息
        Dept dept = redisUtil.getEntityWithCache("dept::", order.getDeptId(), Dept.class,
                deptId -> deptMapper.selectById(deptId));
        if (dept != null) {
            orderDetailVO.setDeptName(dept.getName());
        }

        return orderDetailVO;
    }

    /**
     * 支付订单
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "order",key = "#orderId")//删除订单缓存和号源缓存
    public boolean payOrder(Long orderId) {
        //获取患者id
        Long patientId = BaseContext.getCurrentId();
        if(patientId == null){
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }

        //检查订单是否存在
        Order order = orderService.getOrderById(orderId);
        if(order == null){
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 使用号源锁
        String lockKey = "lock:slot_operation:" + order.getSlotId();
        boolean locked = redisUtil.tryLock(lockKey, 3000, 15000, TimeUnit.MILLISECONDS);
        if (!locked) {
            throw new BaseException("支付处理中，请稍后再试");
        }

        try {
            //检查订单是否存在
            Order currentOrder = orderService.getOrderById(orderId);
            if(currentOrder == null){
                throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
            }

            //验证订单是否属于患者
            if(!currentOrder.getPatientId().equals(patientId)){
                throw new BaseException(MessageConstant.NO_PERMISSION);
            }

            //检查订单状态是否正确
            if(!Order.ORDER_STATUS_PENDING.equals(order.getOrderStatus())){
                if(Order.ORDER_STATUS_PAID.equals(order.getOrderStatus())){
                    throw new BaseException(MessageConstant.ORDER_HAS_BEEN_PAID);
                }else if(Order.ORDER_STATUS_CANCELLED.equals(order.getOrderStatus())){
                    throw new BaseException(MessageConstant.ORDER_CANCEL);
                }else if(Order.ORDER_STATUS_COMPLETED.equals(order.getOrderStatus())){
                    throw new BaseException(MessageConstant.ORDER_CHECK_IN);
                }else {
                    throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
                }
            }

            //模拟支付(这里替换真实支付逻辑)
            Order updatedOrder = new Order();
            updatedOrder.setId(orderId);
            updatedOrder.setOrderStatus(Order.ORDER_STATUS_PAID);
            updatedOrder.setPaymentTime(LocalDateTime.now());
            updatedOrder.setUpdateTime(LocalDateTime.now());

            int result = orderMapper.updateById(updatedOrder);
            if (result <= 0) {
                throw new BaseException(MessageConstant.PAYMENT_FAILED);
            }

            //根据订单类型处理排队逻辑
            queueService.handleQueueAfterPayment(order);

            return true;

        } finally {
            //释放分布式锁
            redisUtil.unlock(lockKey);
        }
    }


    /**
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNo() {
        // 优化订单号生成逻辑，严格控制总长度在20字符以内
        long timestamp = System.currentTimeMillis();

        // 使用36进制缩短时间戳长度（更紧凑）
        String timestampPart = Long.toString(timestamp, 36).toUpperCase();
        // 取时间戳后6位（足够保证唯一性）
        if (timestampPart.length() > 6) {
            timestampPart = timestampPart.substring(timestampPart.length() - 6);
        }

        // 3位随机数（000-999）
        String randomPart = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));

        // 格式: 前缀(5) + 时间戳(6) + 随机数(3) = 14字符，预留空间防止意外超长
        String orderNo = orderPrefix + timestampPart + randomPart;

        // 双重保险：如果超过20字符，则截取前20位
        if (orderNo.length() > 20) {
            orderNo = orderNo.substring(0, 20);
        }

        log.info("生成订单号: {}, 长度: {}", orderNo, orderNo.length());
        return orderNo;
    }

    /**
     * 取消订单
     * @param orderCancelDTO 订单取消信息
     * @return 是否取消成功
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "order",key = "#orderCancelDTO.orderId")//删除订单缓存和号源缓存
    public boolean cancelOrder(OrderCancelDTO orderCancelDTO) {
        //获取患者id
        Long patientId = BaseContext.getCurrentId();
        if(patientId == null){
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }
        Long orderId = orderCancelDTO.getOrderId();
        String cancelReason = orderCancelDTO.getCancelReason();

        // 先查询订单信息
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 验证订单是否属于患者
        if (!order.getPatientId().equals(patientId)) {
            throw new BaseException(MessageConstant.NO_PERMISSION);
        }
        // 使用号源锁
        String lockKey = "lock:slot_operation:" + order.getSlotId();
        boolean locked = redisUtil.tryLock(lockKey, 3000, 15000, TimeUnit.MILLISECONDS);
        if (!locked) {
            throw new BaseException("操作处理中，请稍后再试");
        }

        try {
            // 在锁内再次查询订单（双检锁）
            Order currentOrder = orderService.getOrderById(orderId);
            if (currentOrder == null) {
                throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
            }

            // 检查订单状态是否可以取消
            if (!Order.ORDER_STATUS_PENDING.equals(currentOrder.getOrderStatus())) {
                if (Order.ORDER_STATUS_PAID.equals(currentOrder.getOrderStatus())) {
                    throw new BaseException(MessageConstant.ORDER_HAS_BEEN_PAID);
                } else if (Order.ORDER_STATUS_CANCELLED.equals(currentOrder.getOrderStatus())) {
                    throw new BaseException(MessageConstant.ORDER_CANCEL);
                } else if (Order.ORDER_STATUS_COMPLETED.equals(currentOrder.getOrderStatus())){
                    throw new BaseException(MessageConstant.ORDER_CHECK_IN);
                }
             }

            // 更新订单状态
            Order updatedOrder = new Order();
            updatedOrder.setId(orderId);
            updatedOrder.setOrderStatus(Order.ORDER_STATUS_CANCELLED);
            updatedOrder.setCancelTime(LocalDateTime.now());
            updatedOrder.setCancelReason(cancelReason);
            updatedOrder.setUpdateTime(LocalDateTime.now());

            int result = orderMapper.updateById(updatedOrder);
            if (result <= 0) {
                throw new BaseException("订单状态已变更，请刷新后重试");
            }

            // 无锁恢复号源库存
            boolean cancelSuccess = slotService.cancelSlot(order.getSlotId(), orderId);

            if (!cancelSuccess) {
                log.warn("订单取消时号源库存恢复失败，订单ID: "+order.getId()+", 号源ID:"+
                         order.getSlotId());
                throw new BaseException("订单取消时号源库存恢复失败");
            }

            // 异步移除队列
            queueService.removeFromQueue(orderId);

            return true;

        } finally {
            redisUtil.unlock(lockKey);
        }
    }

}
