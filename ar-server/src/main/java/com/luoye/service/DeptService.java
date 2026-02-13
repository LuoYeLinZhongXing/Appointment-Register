package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.dept.DeptCreateDTO;
import com.luoye.dto.dept.DeptPageQueryDTO;
import com.luoye.dto.dept.DeptStatusUpdateDTO;
import com.luoye.dto.dept.DeptUpdateDTO;
import com.luoye.entity.Dept;

import java.util.List;
import java.util.Map;

public interface DeptService extends IService<Dept> {


    /**
     * 创建科室
     * @param deptCreateDTO 科室创建数据传输对象
     * @return 创建的科室实体
     */
    Dept create(DeptCreateDTO deptCreateDTO);

    /**
     * 分页查询科室
     * @param deptPageQueryDTO 科室分页查询数据传输对象
     * @return 查询结果Map
     */
    Map<String, Object> pageQuery(DeptPageQueryDTO deptPageQueryDTO);

    /**
     * 更新科室信息
     * @param deptUpdateDTO 科室更新数据传输对象
     */
    void update(DeptUpdateDTO deptUpdateDTO);

    /**
     * 删除科室
     * @param id 科室ID
     */
    void delete(Long id);

    /**
     * 更新科室状态
     * @param statusUpdateDTO 状态更新数据传输对象
     */
    void updateStatus(DeptStatusUpdateDTO statusUpdateDTO);

    /**
     * 根据ID查询科室信息
     * @param id 科室ID
     * @return 科室实体
     */
    Dept getById(Long id);

    /**
     * 获取所有启用的科室列表
     * @return 科室列表
     */
    List<Dept> getAllEnabledDepts();

    /**
     * 获取所有科室的ID和名称
     * @return 科室ID和名称列表
     */
    List<Map<String, Object>> getAllDeptIdAndName();

}
