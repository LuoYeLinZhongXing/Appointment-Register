package com.luoye.dto.patient;

import lombok.Data;

@Data
public class PatientForgotPasswordDTO {
    private String phone;
    private String newPassword;
}
