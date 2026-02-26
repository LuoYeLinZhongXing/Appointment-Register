package com.luoye.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单取号DTO
 */
@Data
public class OrderCheckInDTO implements Serializable {
    /**
     * 订单ID
     */
    private Long orderId;
}
