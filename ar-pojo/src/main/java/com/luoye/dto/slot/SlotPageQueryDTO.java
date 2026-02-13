package com.luoye.dto.slot;

import lombok.Data;

@Data
public class SlotPageQueryDTO {
    /**
     * 医生ID（用于筛选当前医生的号源）
     */
    private Long doctorId;

    /**
     * 医生姓名（用于按姓名筛选医生的号源）
     */
    private String doctorName;

    /**
     * 出诊日期范围 - 开始日期
     */
    private java.time.LocalDate startDate;

    /**
     * 出诊日期范围 - 结束日期
     */
    private java.time.LocalDate endDate;

    /**
     * 时间段: morning/afternoon/night
     */
    private String timePeriod;

    /**
     * 号源状态：1.可预约, 0.已停诊, 2.已约满
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;
}
