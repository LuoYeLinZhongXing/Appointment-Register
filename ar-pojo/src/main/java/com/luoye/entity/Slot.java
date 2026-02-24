package com.luoye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 号源实体类
 */
@TableName(value = "slot")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Slot implements Serializable {
    /**
     * 号源状态常量
     */
    public static final Integer STATUS_AVAILABLE = 1; // 可预约
    public static final Integer STATUS_STOPPED = 0; // 已停诊
    public static final Integer STATUS_FULL = 2; // 已约满
    public static final Integer MAX_SLOT_COUNT = 20;
    /**
     * 号源id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 科室id
     */
    @TableField(value = "dept_id")
    private Long deptId;

    /**
     * 医生id
     */
    @TableField(value = "doctor_id")
    private Long doctorId;

    /**
     * 出诊日期
     */
    @TableField(value = "schedule_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /**
     * 时间段: morning/afternoon/night
     */
    @TableField(value = "time_period")
    private String timePeriod;

    /**
     * 挂号费
     */
    @TableField(value = "fee_amount")
    private Integer feeAmount;

    /**
     * 总号源数
     */
    @TableField(value = "total_count")
    private Integer totalCount;

    /**
     * 已预约数
     */
    @TableField(value = "booked_count")
    private Integer bookedCount;

    /**
     * 号源状态：1.可预约, 0.已停诊, 2.已约满
     */
    @TableField(value = "status")
    private Integer status;

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
