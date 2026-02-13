package com.luoye.dto.doctor;

import lombok.Data;

@Data
public class DoctorLoginDTO {
    /**
     * 手机号
     */
    private String phone;
    /**
     * 密码
     */
    private String password;
}

