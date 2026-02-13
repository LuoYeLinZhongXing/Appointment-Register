package com.luoye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 医生实体类
 */
@TableName(value = "doctor")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Doctor implements Serializable {
    /**
     * 医生id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 医生姓名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 性别: 0.男 1.女
     */
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 身份证号
     */
    @TableField(value = "card")
    private String card;

    /**
     * 状态: 1.在职 2.离职
     */
    @TableField(value = "status")
    private Integer status;

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
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
