package com.luoye.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.doctor.*;
import com.luoye.entity.Doctor;
import com.luoye.vo.PageResult;

import java.util.List;
import java.util.Map;


public interface DoctorService extends IService<Doctor> {
    /**
     * 医生注册
     * @param doctorRegisterDTO 医生注册数据传输对象
     */
    void register(DoctorRegisterDTO doctorRegisterDTO);

    /**
     * 医生登录
     * @param doctorLoginDTO 医生登录数据传输对象
     * @return 登录结果，包含token等信息
     */
    Map<String, Object> login(DoctorLoginDTO doctorLoginDTO);

    /**
     * 分页查询医生信息
     * @param doctorPageQueryDTO 分页查询条件
     * @return 包含分页数据的结果
     */
    PageResult<Doctor> pageQuery(DoctorPageQueryDTO doctorPageQueryDTO);

    /**
     * 更新医生信息
     * @param doctorUpdateDTO 医生更新数据传输对象
     */
    void update(DoctorUpdateDTO doctorUpdateDTO);

    /**
     * 删除医生
     * @param id 医生ID
     */
    Doctor delete(Long id);

    /**
     * 更改医生状态
     * @param doctorStatusUpdateDTO 医生状态更新DTO
     */
    Doctor updateStatus(DoctorStatusUpdateDTO doctorStatusUpdateDTO);


    /**
     * 根据ID查询医生信息
     * @param id 医生ID
     * @return 医生实体
     */
    Doctor getById(Long id);

    /**
     * 根据科室ID查询医生信息
     * @param deptId 科室ID
     * @return 医生列表
     */
    List<Doctor> getDoctorsByDeptId(Long deptId);

    /**
     * 医生忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     */
    Doctor forgotPassword(String phone, String newPassword);

}
