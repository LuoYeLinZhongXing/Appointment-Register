package com.luoye.dto.slot;

import lombok.Data;

/**
 * 号源库存信息DTO
 */
@Data
public class SlotInventoryInfo {
    /**
     *  号源ID
     */
    private Long slotId;
    /**
     * 总库存数
     */
    private Integer totalCount;
    /**
     * 已预约数
     */
    private Integer bookedCount;
    /**
     * 剩余库存数
     */
    private Integer remainingCount;
    /**
     * 状态
     */
    private Integer status;
}
