package com.luoye.dto.patient;

import lombok.Data;

@Data
public class PatientUpdateDTO {
    /**
     * 患者id
     */
    private Long id;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 性别 :0.男 1.女
     */
    private Integer gender;

    /**
     * 身份证号
     */
    private String card;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 地址
     */
    private String address;
}
