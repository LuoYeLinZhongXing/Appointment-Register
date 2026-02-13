package com.luoye.dto.doctor;

import lombok.Data;

@Data
public class DoctorStatusUpdateDTO {
    /**
     * 医生id
     */
    private Long id;

    /**
     * 医生状态: 1.在职 2.离职
     */
    private Integer status;
}
