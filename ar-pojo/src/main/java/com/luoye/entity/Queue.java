package com.luoye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 排队实体类
 */
@TableName(value = "queue")
@Data
public class Queue implements Serializable {
    /**
     * 排队状态常量
     */
    public static final Integer QUEUE_STATUS_WAITING = 1; // 等待中
    public static final Integer QUEUE_STATUS_CALLING = 2; // 呼叫中
    public static final Integer QUEUE_STATUS_TREATING = 3; // 就诊中
    public static final Integer QUEUE_STATUS_COMPLETED = 4; // 已完成
    public static final Integer QUEUE_STATUS_MISSED = 5; // 过号
    public static final Integer QUEUE_STATUS_CANCELLED = 6; // 已取消

    /**
     * 排队id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单id
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 患者id
     */
    @TableField(value = "patient_id")
    private Long patientId;

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
     * 是否优先级：1是, 0否
     */
    @TableField(value = "is_priority")
    private Integer isPriority;

    /**
     * 排队号码(每个医生独立)
     */
    @TableField(value = "queue_number")
    private Integer queueNumber;

    /**
     * 排队状态：1等待中, 2呼叫中, 3就诊中, 4已完成, 5过号, 6已取消
     */
    @TableField(value = "queue_status")
    private Integer queueStatus;

    /**
     * 报到时间
     */
    @TableField(value = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 呼叫时间
     */
    @TableField(value = "call_time")
    private LocalDateTime callTime;

    /**
     * 开始就诊时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束就诊时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    /**
     * 过号次数
     */
    @TableField(value = "missed_count")
    private Integer missedCount;

    /**
     * 最大允许过号次数（默认3次）
     */
    @TableField(value = "max_missed_allowed")
    private Integer maxMissedAllowed;

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
