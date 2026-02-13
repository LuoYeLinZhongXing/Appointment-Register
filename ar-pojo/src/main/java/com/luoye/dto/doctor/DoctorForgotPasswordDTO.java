package com.luoye.dto.doctor;

import lombok.Data;

@Data
public class DoctorForgotPasswordDTO {
    private String phone;
    private String newPassword;
}
