package com.luoye.dto.order;

import lombok.Data;

@Data
public class OrderPageQueryDTO {
    /**
     * 订单号（模糊查询）
     */
    private String orderNo;

    /**
     * 号源ID
     */
    private Long slotId;

    /**
     * 订单状态
     */
    private Integer orderStatus;

    /**
     * 是否急诊：0普通，1急诊
     */
    private Integer isEmergency;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 排序字段
     */
    private String sortBy = "createTime";

    /**
     * 排序方向
     */
    private String sortDir = "desc";
}
