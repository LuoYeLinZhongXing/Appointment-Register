package com.luoye.dto.admin;

import lombok.Data;

/**
 * 管理员注册数据传输对象
 */
@Data
public class AdminRegisterDTO {
    /**
     * 管理员姓名
     */
    private String name;

    /**
     * 密码
     */
    private String password;

    /**
     * 性别 (0-女, 1-男)
     */
    private Integer gender;

    /**
     * 身份证号
     */
    private String card;

    /**
     * 手机号码
     */
    private String phone;
}
