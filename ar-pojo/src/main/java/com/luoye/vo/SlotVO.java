package com.luoye.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 号源信息返回类
 */
@Data
public class SlotVO {
    /**
     * 号源id
     */
    private Long id;

    /**
     * 科室id
     */
    private Long deptId;

    /**
     * 医生id
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生职位
     */
    private Integer doctorPost;

    /**
     * 医生挂号费
     */
    private BigDecimal feeAmount;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 出诊日期
     */
    private LocalDate scheduleDate;

    /**
     * 时间段: morning/afternoon/night
     */
    private String timePeriod;

    /**
     * 总号源数
     */
    private Integer totalCount;

    /**
     * 已预约数
     */
    private Integer bookedCount;

    /**
     * 剩余号源数
     */
    private Integer remainingCount;

    /**
     * 号源状态：1.可预约, 0.已停诊, 2.已约满
     */
    private Integer status;

    /**
     * 医生简介
     */
    private String doctorIntroduction;
}
