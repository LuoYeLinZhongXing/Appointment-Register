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
 * 日志实体类
 */
@TableName(value = "log")
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Log implements Serializable {
    /**
     * 日志id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型: 增删改查
     */
    @TableField(value = "log_type")
    private String logType;

    /**
     * 操作人类型: 管理员/医生/患者/系统
     */
    @TableField(value = "operator_type")
    private String operatorType;

    /**
     * 操作人id
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 目标类型 : 订单/号源 等
     */
    @TableField(value = "target_type")
    private String targetType;

    /**
     * 目标id
     */
    @TableField(value = "target_id")
    private Long targetId;

    /**
     * 操作详情
     */
    @TableField(value = "operation_detail")
    private String operationDetail;

    /**
     * ip地址
     */
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 操作结果 : 0.失败 1.成功
     */
    @TableField(value = "success_flag")
    private Integer successFlag;

    /**
     * 失败消息
     */
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}
