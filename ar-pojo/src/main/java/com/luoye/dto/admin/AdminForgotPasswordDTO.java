package com.luoye.dto.admin;

import lombok.Data;

@Data
public class AdminForgotPasswordDTO {
    private String phone;
    private String newPassword;
}
