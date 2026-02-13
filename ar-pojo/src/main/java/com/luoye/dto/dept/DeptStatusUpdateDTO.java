package com.luoye.dto.dept;


import lombok.Data;

// 创建专门的DTO类
@Data
public class DeptStatusUpdateDTO {

    /**
     * 科室ID
     */
    private Long id;

    /**
     * 科室状态
     */
    private Integer status;
}
