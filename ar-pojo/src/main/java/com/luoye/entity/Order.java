package com.luoye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 订单实体类
 */
@TableName(value = "`order`")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Order implements Serializable {
    /**
     * 订单状态常量
     */
    public static final Integer ORDER_STATUS_PENDING = 1; // 待支付
    public static final Integer ORDER_STATUS_PAID = 2; // 已支付
    public static final Integer ORDER_STATUS_CANCELLED = 3; // 已取消
    public static final Integer ORDER_STATUS_COMPLETED = 4; // 已就诊
    public static final Integer ORDER_STATUS_CHECKED_IN = 5; // 已取号


    /**
     * 急诊标识常量
     */
    public static final Integer EMERGENCY_NORMAL = 0; // 普通
    public static final Integer EMERGENCY_URGENT = 1; // 急诊

    /**
     * 订单id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField(value = "order_no")
    private String orderNo;

    /**
     * 患者id
     */
    @TableField(value = "patient_id")
    private Long patientId;

    /**
     * 号源id
     */
    @TableField(value = "slot_id")
    private Long slotId;

    /**
     * 医生id
     */
    @TableField(value = "doctor_id")
    private Long doctorId;

    /**
     * 科室id
     */
    @TableField(value = "dept_id")
    private Long deptId;

    /**
     * 挂号费
     */
    @TableField(value = "fee_amount")
    private Integer  feeAmount;

    /**
     * 订单状态：1.待支付 2.已支付 3.已取消 4.已就诊 5.已取号
     */
    @TableField(value = "order_status")
    private Integer orderStatus;

    /**
     * 支付时间
     */
    @TableField(value = "payment_time")
    private LocalDateTime paymentTime;

    /**
     * 取号/报到时间
     */
    @TableField(value = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 取消时间
     */
    @TableField(value = "cancel_time")
    private LocalDateTime cancelTime;

    /**
     * 是否急诊：0普通，1急诊
     */
    @TableField(value = "is_emergency")
    private Integer isEmergency;

    /**
     * 取消原因
     */
    @TableField(value = "cancel_reason")
    private String cancelReason;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
