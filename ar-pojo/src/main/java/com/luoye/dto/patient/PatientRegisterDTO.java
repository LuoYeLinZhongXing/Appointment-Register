package com.luoye.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegisterDTO {
    /**
     * 患者姓名
     */
    private String name;
    /**
     * 患者手机号
     */
    private String phone;
    /**
     * 患者密码
     */
    private String password;
    /**
     *  性别
     */
    private Integer gender;
    /**
     * 身份证号码
     */
    private String card;
    /**
     * 邮箱
     */
    private String email;
    /**
     *  地址
     */
    private String address;


}
