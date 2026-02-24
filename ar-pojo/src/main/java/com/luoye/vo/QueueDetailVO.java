package com.luoye.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 队列详情视图对象
 */
@Data
public class QueueDetailVO {

    /**
     * 队列ID
     */
    private Long id;
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 患者名称
     */
    private String patientName;

    /**
     * 是否优先级：1是, 0否
     */
    private Integer isPriority;

    /**
     * 排队号码(每个医生独立)
     */
    private Integer queueNumber;

    /**
     * 排队状态：1等待中, 2呼叫中, 3就诊中, 4已完成, 5过号, 6已取消
     */
    private Integer queueStatus;

    /**
     * 报到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 呼叫时间
     */
    private LocalDateTime callTime;

    /**
     * 开始就诊时间
     */
    private LocalDateTime startTime;

    /**
     * 结束就诊时间
     */
    private LocalDateTime endTime;

    /**
     * 过号次数
     */
    private Integer missedCount;

    /**
     * 最大允许过号次数（默认3次）
     */
    private Integer maxMissedAllowed;
}
