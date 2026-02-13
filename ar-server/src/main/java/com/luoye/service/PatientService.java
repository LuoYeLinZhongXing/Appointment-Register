package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.patient.PatientLoginDTO;
import com.luoye.dto.patient.PatientQueryDTO;
import com.luoye.dto.patient.PatientRegisterDTO;
import com.luoye.dto.patient.PatientUpdateDTO;
import com.luoye.entity.Patient;
import com.luoye.vo.PageResult;

import java.util.Map;

public interface PatientService extends IService<Patient> {

    /**
     * 患者注册
     * @param patientRegisterDTO 注册信息
     */
    void register(PatientRegisterDTO patientRegisterDTO);

    /**
     * 患者登录
     * @param patientLoginDTO 登录信息
     * @return 登录结果，包含token等信息
     */
    Map<String, Object> login(PatientLoginDTO patientLoginDTO);

    /**
     * 更新患者信息
     * @param patientUpdateDTO 更新信息
     */
    void update(PatientUpdateDTO patientUpdateDTO);

    /**
     * 删除患者
     * @param id 患者ID
     */
    void delete(Long id);

    /**
     * 根据ID查询患者信息
     * @param id 患者ID
     * @return 患者实体
     */
    Patient getById(Long id);

    /**
     * 患者忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     */
    Long forgotPassword(String phone, String newPassword);

    /**
     * 分页查询患者信息
     * @param patientQueryDTO 查询条件
     * @return 分页结果
     */
    PageResult<Patient> pageQuery(PatientQueryDTO patientQueryDTO);


}
