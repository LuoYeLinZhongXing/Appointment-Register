package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.admin.AdminLoginDTO;
import com.luoye.dto.admin.AdminRegisterDTO;
import com.luoye.dto.admin.AdminUpdateDTO;
import com.luoye.entity.Admin;

import java.util.Map;

public interface AdminService extends IService<Admin> {

    /**
     * 管理员注册
     * @param adminRegisterDTO 注册信息
     */
    void register(AdminRegisterDTO adminRegisterDTO);

    /**
     * 管理员登录
     * @param adminLoginDTO 登录信息
     * @return 登录结果，包含token等信息
     */
    Map<String, Object> login(AdminLoginDTO adminLoginDTO);

    /**
     * 根据ID查询管理员信息
     * @param id 管理员ID
     * @return 管理员实体
     */
    Admin getById(Long id);

    /**
     * 更新管理员信息
     * @param adminUpdateDTO 管理员更新数据传输对象
     */
    void update(AdminUpdateDTO adminUpdateDTO);

    /**
     * 管理员忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     */
    Long forgotPassword(String phone, String newPassword);

}
