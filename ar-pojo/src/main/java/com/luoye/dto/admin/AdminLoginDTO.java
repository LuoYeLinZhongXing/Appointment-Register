package com.luoye.dto.admin;

import lombok.Data;

/**
 * 管理员登录数据传输对象
 */
@Data
public class AdminLoginDTO {
    /**
     * 管理员手机号
     */
    private String phone;

    /**
     * 管理员密码
     */
    private String password;
}
