package com.luoye.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详细信息视图对象
 */
@Data
public class OrderDetailVO {
    /**
     * 订单ID
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
     * 医生名称
     */
    private String doctorName;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 挂号费
     */
    private BigDecimal feeAmount;

    /**
     * 订单状态：1.待支付 2.已支付 3.已取消 4.已就诊
     */
    private Integer orderStatus;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 取号/报到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 是否急诊：0普通，1急诊
     */
    private Integer isEmergency;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
