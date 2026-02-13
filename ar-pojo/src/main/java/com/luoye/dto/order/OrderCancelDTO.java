package com.luoye.dto.order;

import lombok.Data;

@Data
public class OrderCancelDTO {
    /**
     *  订单ID
     */
    private Long orderId;

    /**
     * 取消原因
     */
    private String cancelReason;
}
