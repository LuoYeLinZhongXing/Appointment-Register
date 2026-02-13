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
 * 科室实体类
 */
@TableName(value = "dept")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Dept implements Serializable {
    /**
     * 科室id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 科室名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 科室类型 :0.临床 1.医技
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 科室位置
     */
    @TableField(value = "location")
    private String location;

    /**
     * 科室状态: 0.停用 1.启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 简介
     */
    @TableField(value = "description")
    private String description;

    /**
     * 负责人id
     */
    @TableField(value = "director_id")
    private Long directorId;

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
