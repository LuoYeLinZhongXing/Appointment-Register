package com.luoye.dto.doctor;

import lombok.Data;

@Data
public class DoctorCallDTO {

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 呼叫类型 NORMAL或者URGENT默认为普通呼叫
     */
    private String callType="NORMAL";
}
