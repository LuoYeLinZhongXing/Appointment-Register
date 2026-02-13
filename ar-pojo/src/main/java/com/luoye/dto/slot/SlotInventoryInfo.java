package com.luoye.dto.slot;

import lombok.Data;

/**
 * 号源库存信息DTO
 */
@Data
public class SlotInventoryInfo {
    private Long slotId;
    private Integer totalCount;
    private Integer bookedCount;
    private Integer remainingCount;
    private Integer status;
}
