package com.luoye.dto.slot;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 放号数据传输对象
 */
@Data
public class SlotReleaseDTO {
    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 出诊日期
     */
    private LocalDate scheduleDate;

    /**
     * 时间段: morning/afternoon/night
     */
    private String timePeriod;

    /**
     * 挂号费
     */
    private Integer feeAmount;

    /**
     * 总号源数
     */
    private Integer totalCount;
}
