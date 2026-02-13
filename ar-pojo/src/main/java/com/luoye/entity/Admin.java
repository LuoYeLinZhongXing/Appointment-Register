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
 * 管理员实体类
 */
@TableName(value = "admin")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Admin implements Serializable {
    /**
     * 管理员id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 管理员名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 管理员密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 性别：0男 1女
     */
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 身份证号
     */
    @TableField(value = "card")
    private String card;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

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
