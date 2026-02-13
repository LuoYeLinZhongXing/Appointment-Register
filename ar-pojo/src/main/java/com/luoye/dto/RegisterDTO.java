package com.luoye.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterDTO {

    /**
     * 号源id
     */
    private Long slotId;

    /**
     * 是否急诊：0普通，1急诊
     */
    private Integer isEmergency;
}
