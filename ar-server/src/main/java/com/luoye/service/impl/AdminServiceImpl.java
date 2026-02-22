package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.admin.AdminLoginDTO;
import com.luoye.dto.admin.AdminRegisterDTO;
import com.luoye.dto.admin.AdminUpdateDTO;
import com.luoye.entity.Admin;
import com.luoye.exception.BaseException;
import com.luoye.mapper.AdminMapper;
import com.luoye.service.AdminService;
import com.luoye.util.JwtUtil;
import com.luoye.util.RedisUtil;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Log
@EnableCaching
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    @Lazy
    private  AdminService adminService;


    /**
     * 管理员注册
     * @param adminRegisterDTO 管理员注册数据传输对象
     */
    @Override
    @Transactional(timeout = 3,rollbackFor = Exception.class)
    public void register(AdminRegisterDTO adminRegisterDTO) {

        // 验证必要参数
        if(adminRegisterDTO.getPhone() == null || adminRegisterDTO.getPassword() == null){
            throw new BaseException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }
        // 验证手机号格式
        if (!adminRegisterDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BaseException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        // 验证密码长度
        if (adminRegisterDTO.getPassword().length() < 6 || adminRegisterDTO.getPassword().length() > 20) {
            throw new BaseException("密码长度必须在6-20位之间");
        }

        // 判断手机号是否已注册
        Long count = adminMapper.selectCount(new QueryWrapper<Admin>().eq("phone", adminRegisterDTO.getPhone()));
        if(count > 0) {
            throw new BaseException(MessageConstant.PHONE_ERROR);
        }
        //创建管理员实体
        Admin admin = new Admin();
        BeanUtils.copyProperties(adminRegisterDTO, admin);
        admin.setPassword(DigestUtils.md5DigestAsHex(admin.getPassword().getBytes()));
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        //插入数据库
        int result = adminMapper.insert(admin);
        if(result <= 1){
            throw new BaseException(MessageConstant.ADMIN_REGISTER_FAILED);
        }

        log.info("管理员注册成功，手机号:"+ adminRegisterDTO.getPhone());
    }

    /**
     * 管理员登录
     * @param adminLoginDTO 管理员登录数据传输对象
     * @return 登录结果Map
     */
    @Override
    public Map<String, Object> login(AdminLoginDTO adminLoginDTO) {
        // 参数验证
        if (adminLoginDTO.getPhone() == null || adminLoginDTO.getPassword() == null) {
            throw new BaseException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }

        // MD5 加密密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(adminLoginDTO.getPassword().getBytes());

        // 查询用户
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>()
                .eq("phone", adminLoginDTO.getPhone())
                .eq("password", encryptedPassword));

        if(admin == null) {
            throw new BaseException(MessageConstant.LOGIN_ERROR);
        }

        admin.setPassword(null);
        //生成jwt令牌
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getId());
        claims.put("phone", admin.getPhone());
        String token = JwtUtil.createToken(claims);

        // 重要：登录成功后设置BaseContext，供本次请求使用
        BaseContext.setCurrentId(admin.getId(), "ADMIN");

        //生成返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", admin);


        log.info("管理员登录成功，手机号:"+ adminLoginDTO.getPhone());
        return result;
    }

    /**
     * 根据ID查询管理员信息
     * @param id 管理员ID
     * @return 管理员实体
     */
    @Override
    @Cacheable(value = "admin", key = "#id",unless = "#result ==null")
    public Admin getById(Long id) {
        // 验证参数是否为空
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        //查询缓存
        return adminMapper.selectById(id);
    }

    /**
     * 更新管理员信息
     * @param adminUpdateDTO 管理员更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class ,timeout = 2)
    @CacheEvict(value = "admin", key = "T(com.luoye.context.BaseContext).getCurrentId()")
    public void update(AdminUpdateDTO adminUpdateDTO) {
        // 获取当前登录的管理员ID
        Long currentAdminId = BaseContext.getCurrentId();

        // 验证必要参数
        if (currentAdminId == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        // 验证性别
        if (adminUpdateDTO.getGender() != null &&
                adminUpdateDTO.getGender() != 0 &&
                adminUpdateDTO.getGender() != 1) {
            throw new BaseException("性别必须为0(男)或1(女)");
        }

        // 验证姓名
        if (adminUpdateDTO.getName() != null) {
            String name = adminUpdateDTO.getName().trim();
            if (name.isEmpty()) {
                throw new BaseException(MessageConstant.NAME_NOT_NULL);
            }
            if (name.length() > 50) {
                throw new BaseException(MessageConstant.NAME_LENGTH_ERROR);
            }
        }

        // 检查管理员是否存在
        Admin existingAdmin = adminService.getById(currentAdminId);
        if (existingAdmin == null) {
            throw new BaseException(MessageConstant.ADMIN_NOT_EXIST);
        }

        // 验证手机号格式
        if (adminUpdateDTO.getPhone() != null) {
            if (!adminUpdateDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
                throw new BaseException(MessageConstant.PHONE_FORMAT_ERROR);
            }

            // 检查手机号是否被其他管理员使用
            QueryWrapper<Admin> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", adminUpdateDTO.getPhone());
            wrapper.ne("id", currentAdminId);
            Long count = adminMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BaseException(MessageConstant.PHONE_ERROR);
            }
        }


        // 更新管理员信息
        Admin adminToUpdate = new Admin();
        adminToUpdate.setId(currentAdminId); // 确保设置ID
        BeanUtils.copyProperties(adminUpdateDTO, adminToUpdate);
        adminToUpdate.setUpdateTime(LocalDateTime.now());

        int rows = adminMapper.updateById(adminToUpdate);
        if (rows == 0) {
            throw new BaseException(MessageConstant.UPDATE_FAILED);
        }

        log.info("管理员更新成功，管理员ID: " + currentAdminId);
    }

    /**
     * 管理员忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     * @return 管理员id
     */
    @Override
    @Transactional(rollbackFor = Exception.class ,timeout = 2)
    @CacheEvict(value = "admin", key = "#result")
    public Long forgotPassword(String phone, String newPassword) {
        // 验证手机号和新密码
        if (phone == null || phone.trim().isEmpty()) {
            throw new BaseException(MessageConstant.PHONE_ERROR);
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BaseException(MessageConstant.NEW_PASSWORD_EMPTY);
        }

        // 验证手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BaseException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        // 验证新密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BaseException("密码长度必须在6-20位之间");
        }

        // 查询手机号是否存在
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("phone", phone));
        if (admin == null) {
            throw new BaseException(MessageConstant.PHONE_NOT_FOUND);
        }

        // 更新密码
        Admin adminToUpdate = new Admin();
        adminToUpdate.setId(admin.getId());
        adminToUpdate.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        adminToUpdate.setUpdateTime(LocalDateTime.now());

        int result = adminMapper.updateById(adminToUpdate);
        if (result == 0) {
            throw new BaseException(MessageConstant.UPDATE_FAILED);
        }
        logger.info("管理员 {} 密码重置成功", phone);

        return admin.getId();
    }
}
