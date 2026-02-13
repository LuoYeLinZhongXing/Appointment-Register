package com.luoye.dto.doctor;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class DoctorUpdateDTO {
    /**
     * 医生id
     */
    private Long id;

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 身份证号
     */
    private String card;

    /**
     * 医生职位: 0.普通医生 1.科室主任
     */
    @TableField(value = "post")
    private Integer post;
    /**
     * 医生简介
     */
    @TableField(value = "introduction")
    private String introduction;

    /**
     * 隶属科室
     */
    @TableField(value = "dept_id")
    private Long deptId;

    /**
     * 状态: 1.在职 2.离职
     */
    private Integer status;
}
