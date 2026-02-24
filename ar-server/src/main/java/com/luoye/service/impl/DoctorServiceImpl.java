package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.constant.MessageConstant;
import com.luoye.dto.doctor.*;
import com.luoye.entity.Doctor;
import com.luoye.exception.BaseException;
import com.luoye.exception.LoginException;
import com.luoye.exception.PhoneRepetitionException;
import com.luoye.mapper.DoctorMapper;
import com.luoye.service.DoctorService;
import com.luoye.util.JwtUtil;
import com.luoye.util.RedisUtil;
import com.luoye.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@EnableCaching
@Slf4j
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    @Lazy
    private DoctorService doctorService;

    @Autowired
    private RedisUtil redisUtil;


    /**
     * 医生注册
     * @param doctorRegisterDTO 医生注册数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(DoctorRegisterDTO doctorRegisterDTO) {
        // 验证必要参数
        String basicValidationError = validateDoctorBasicInfo(doctorRegisterDTO);
        if (basicValidationError != null) {
            throw new BaseException(basicValidationError);
        }

        //创建医生对象
        Doctor doctor = new Doctor();
        BeanUtils.copyProperties(doctorRegisterDTO, doctor);



        // 对密码进行md5加密
        doctor.setPassword(DigestUtils.md5DigestAsHex(doctor.getPassword().getBytes()));

        // 默认设置状态为1
        doctor.setStatus(1);
        // 如果未设置职位，默认为普通医生（0）
        if(doctor.getPost() == null) {
            doctor.setPost(0); // 默认为普通医生
        }
        doctor.setCreateTime(LocalDateTime.now());
        doctor.setUpdateTime(LocalDateTime.now());
        int insert = doctorMapper.insert(doctor);
        if(insert <= 0){
            throw new BaseException(MessageConstant.REGISTER_ERROR);
        }
    }

    /**
     * 验证医生基本信息
     * @param doctorRegisterDTO 医生注册DTO
     * @return 验证错误信息，null表示验证通过
     */
    private String validateDoctorBasicInfo(DoctorRegisterDTO doctorRegisterDTO) {
        // 验证必要参数
        if(doctorRegisterDTO.getPhone() == null || doctorRegisterDTO.getPassword() == null){
            return  MessageConstant.PHONE_PASSWORD_NOT_NULL;
        }

        // 判断手机号是否已注册
        Long count = doctorMapper.selectCount(new QueryWrapper<Doctor>().eq("phone", doctorRegisterDTO.getPhone()));
        if(count > 0)
            return MessageConstant.PHONE_ERROR;

        // 验证姓名
        if (doctorRegisterDTO.getName() == null || doctorRegisterDTO.getName().trim().isEmpty()) {
            return MessageConstant.DOCTOR_NAME_EMPTY;
        }

        if (doctorRegisterDTO.getName().trim().length() > 50) {
            return MessageConstant.DOCTOR_NAME_TOO_LONG;
        }

        // 验证手机号格式
        if (!doctorRegisterDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
            return MessageConstant.PHONE_FORMAT_ERROR;
        }


        // 验证密码
        if (doctorRegisterDTO.getPassword().trim().isEmpty()) {
            return  MessageConstant.PASSWORD_EMPTY;
        }

        // 验证科室ID
        if (doctorRegisterDTO.getDeptId() == null) {
            return MessageConstant.DEPT_ID_EMPTY;
        }

        // 验证性别值
        if (doctorRegisterDTO.getGender() != null &&
                doctorRegisterDTO.getGender() != 0 &&
                doctorRegisterDTO.getGender() != 1) {
            return  MessageConstant.GENDER_INVALID;
        }

        // 验证职位值
        if (doctorRegisterDTO.getPost() != null &&
                doctorRegisterDTO.getPost() != 0 &&
                doctorRegisterDTO.getPost() != 1) {
            return MessageConstant.POST_INVALID;
        }

        // 验证身份证号格式（如果提供了的话）
        if (doctorRegisterDTO.getCard() != null && !doctorRegisterDTO.getCard().trim().isEmpty()) {
            if (!doctorRegisterDTO.getCard().matches("^\\d{17}[\\dXx]$")) {
                return MessageConstant.CARD_FORMAT_ERROR;
            }
            // 验证身份证号唯一性
            Long cardCount = doctorMapper.selectCount(new QueryWrapper<Doctor>().eq("card", doctorRegisterDTO.getCard().trim()));
            if (cardCount > 0) {
                return "身份证号已存在，请使用其他身份证号";
            }
        }

        // 如果要注册为科室主任，验证科室主任唯一性
        if (doctorRegisterDTO.getPost() != null && doctorRegisterDTO.getPost() == 1) {
            return validateDepartmentHeadUniqueness(doctorRegisterDTO.getDeptId());
        }
        return null; // 验证通过
    }

    /**
     * 验证科室主任唯一性
     * @param deptId 科室ID
     * @return 验证错误信息，null表示验证通过
     */
    private String validateDepartmentHeadUniqueness(Long deptId) {
        // 查询该科室已有的在职主任数量
        QueryWrapper<Doctor> headQuery = new QueryWrapper<>();
        headQuery.eq("dept_id", deptId)
                .eq("post", 1)
                .eq("status", 1); // 只统计在职的主任

        Long existingHeadCount = doctorMapper.selectCount(headQuery);

        if (existingHeadCount > 0) {
            return "该科室已存在主任，无法重复设置";
        }

        return null; // 验证通过
    }

    /**
     * 医生登录
     * @param doctorLoginDTO 医生登录数据传输对象
     * @return 登录结果Map
     */
    @Override
    public Map<String, Object> login(DoctorLoginDTO doctorLoginDTO) {
        // 参数验证
        if (doctorLoginDTO.getPhone() == null || doctorLoginDTO.getPassword() == null) {
            throw new BaseException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }

        doctorLoginDTO.setPassword(DigestUtils.md5DigestAsHex(doctorLoginDTO.getPassword().getBytes()));
        Doctor doctor = doctorMapper.selectOne(new QueryWrapper<Doctor>()
                .eq("phone", doctorLoginDTO.getPhone())
                .eq("password", doctorLoginDTO.getPassword()));
        if(doctor == null)
            throw new BaseException(MessageConstant.LOGIN_ERROR);
        doctor.setPassword(null);
        //生成jwt令牌
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("doctorId", doctor.getId());
        claims.put("phone", doctor.getPhone());
        String token = JwtUtil.createToken(claims);
        //生成token
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("doctor", doctor);
        //返回结果
        return result;
    }

    /**
     * 分页查询医生信息
     * @param doctorPageQueryDTO 医生分页查询数据传输对象
     * @return 查询结果Map
     */
    @Override
    public PageResult<Doctor> pageQuery(DoctorPageQueryDTO doctorPageQueryDTO) {
        // 创建分页对象
        Page<Doctor> page = new Page<>(doctorPageQueryDTO.getPage(),doctorPageQueryDTO.getPageSize());
        //构建查询条件
        QueryWrapper<Doctor> doctorQueryWrapper = new QueryWrapper<>();

        //按照id查询
        if(doctorPageQueryDTO.getId() !=null) {
            doctorQueryWrapper.eq("id", doctorPageQueryDTO.getId());
        }

        //按照姓名查询(模糊匹配)
        if(doctorPageQueryDTO.getName() !=null && !doctorPageQueryDTO.getName().isEmpty()){
            doctorQueryWrapper.like("name", doctorPageQueryDTO.getName());
        }

        //按照职位查询
        if(doctorPageQueryDTO.getPost() !=null){
            doctorQueryWrapper.eq("post", doctorPageQueryDTO.getPost());
        }
        //按照状态查询
        if(doctorPageQueryDTO.getStatus() !=null){
            doctorQueryWrapper.eq("status", doctorPageQueryDTO.getStatus());
        }

        //按照科室id查询
        if(doctorPageQueryDTO.getDeptId() !=null){
            doctorQueryWrapper.eq("dept_id", doctorPageQueryDTO.getDeptId());
        }

        //设置排序规则
        String sortBy = doctorPageQueryDTO.getSortBy();
        if(sortBy ==null || sortBy.isEmpty()){
            //默认按照创建时间排序
            sortBy="create_time";
        }else {
            // 验证排序字段是否有效
            if (!isValidSortField(sortBy)) {
                throw new BaseException(MessageConstant.SORT_FIELD_INVALID);
            }
        }

        //设置排序方式
        if("asc".equalsIgnoreCase(doctorPageQueryDTO.getSortDir())){
            doctorQueryWrapper.orderByAsc(sortBy);
        }else {
            doctorQueryWrapper.orderByDesc(sortBy);
        }

        //执行查询
        Page<Doctor> doctorPage = doctorMapper.selectPage(page, doctorQueryWrapper);

        PageResult<Doctor> doctorPageResult = new PageResult<>();
        doctorPageResult.setTotal(doctorPage.getTotal());
        doctorPageResult.setRecords(doctorPage.getRecords());
        doctorPageResult.setPages((int)doctorPage.getPages());
        doctorPageResult.setCurrent((int) doctorPage.getCurrent());
        doctorPageResult.setSize((int) doctorPage.getSize());

        return doctorPageResult;
    }

    /**
     * 验证排序字段是否有效
     * @param field 排序字段名
     * @return 是否有效
     */
    private boolean isValidSortField(String field) {
        // 定义允许的排序字段
        Set<String> validFields = Set.of(
                "id", "name", "phone", "gender", "status",
                "post", "dept_id", "create_time", "update_time"
        );
        return validFields.contains(field.toLowerCase());
    }

    /**
     * 更新医生信息
     * @param doctorUpdateDTO 医生更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "doctor", key = "#doctorUpdateDTO.id"),
                    @CacheEvict(value = "doctor_dept", key = "#doctorUpdateDTO.deptId",
                            condition = "#doctorUpdateDTO.deptId != null")
            }
    )
    public void update(DoctorUpdateDTO doctorUpdateDTO) {
        //检查必要参数
        if (doctorUpdateDTO.getId() == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        //检查姓名是否合法
        if (doctorUpdateDTO.getName() != null) {
            String name = doctorUpdateDTO.getName().trim();
            if (name.isEmpty()) {
                throw new BaseException(MessageConstant.DOCTOR_NAME_EMPTY);
            }
            if (name.length() > 50) {
                throw new BaseException(MessageConstant.DOCTOR_NAME_TOO_LONG);
            }
        }

        // 使用缓存获取医生信息
        Doctor existingDoctor = doctorService.getById(doctorUpdateDTO.getId());
        if (existingDoctor == null) {
            throw new BaseException(MessageConstant.DOCTOR_NOT_FOUND);
        }

        // 检查手机号是否被其他医生使用
        if (doctorUpdateDTO.getPhone() != null) {
            QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", doctorUpdateDTO.getPhone());
            wrapper.ne("id", doctorUpdateDTO.getId()); // 排除当前医生自身
            Long count = doctorMapper.selectCount(wrapper);
            if (count > 0) {
                throw new PhoneRepetitionException(MessageConstant.PHONE_ERROR);
            }
        }

        // 检查职位变更时的科室主任唯一性
        if (doctorUpdateDTO.getPost() != null && doctorUpdateDTO.getPost() == 1) {
            // 如果要设置为主任，需要验证科室主任唯一性
            Long targetDeptId = doctorUpdateDTO.getDeptId() != null ?
                    doctorUpdateDTO.getDeptId() : existingDoctor.getDeptId();

            String headValidationError = validateDepartmentHeadUniqueness(targetDeptId);
            if (headValidationError != null) {
                throw new BaseException(headValidationError);
            }
        }

        // 复制属性到现有医生对象
        BeanUtils.copyProperties(doctorUpdateDTO, existingDoctor, "password", "createTime");


        // 单独处理职位字段，如果是字符串则转换为整型
        if (doctorUpdateDTO.getPost() != null) {
            existingDoctor.setPost(doctorUpdateDTO.getPost());
        }
        // 设置更新时间
        existingDoctor.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        doctorMapper.updateById(existingDoctor);

    }

    /**
     * 删除医生
     * @param id 医生ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "doctor", key = "#id"),
                    @CacheEvict(value = "doctor_dept", key = "#result.deptId",
                                condition = "result != null")
            }
    )
    public Doctor delete(Long id) {
        //检查必要参数
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        // 使用缓存获取医生信息
        Doctor existingDoctor = doctorService.getById(id);
        if (existingDoctor == null) {
            throw new BaseException(MessageConstant.DOCTOR_NOT_FOUND);
        }

        //删除
        int result = doctorMapper.deleteById(id);
        if(result <= 0){
            throw new BaseException(MessageConstant.DELETE_FAILED);
        }

        return existingDoctor;

    }

    /**
     * 更新医生状态
     * @param doctorStatusUpdateDTO 医生状态更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "doctor", key = "#doctorStatusUpdateDTO.id"),
                    @CacheEvict(value = "doctor_dept", key = "#result.deptId")
            }
    )
    public Doctor updateStatus(DoctorStatusUpdateDTO doctorStatusUpdateDTO) {
        //检查必要参数
        if(doctorStatusUpdateDTO.getStatus() == null || doctorStatusUpdateDTO.getId() == null){
            log.error("状态更新参数为空 - ID:  "+doctorStatusUpdateDTO.getId()+", Status: "+
                      doctorStatusUpdateDTO.getStatus());
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        //验证状态值是否合法
        Integer status = doctorStatusUpdateDTO.getStatus();
        if(status != 1 && status != 2){
            log.error("状态值错误 - 传入状态值: "+status+", 允许值: 1(在职) 或 2(离职)");
            throw new BaseException(MessageConstant.STATUS_INVALID);
        }

        // 使用缓存获取医生信息
        Doctor existingDoctor = doctorService.getById(doctorStatusUpdateDTO.getId());
        if(existingDoctor == null){
            throw new BaseException(MessageConstant.DOCTOR_NOT_FOUND);
        }

        existingDoctor.setStatus(status);
        existingDoctor.setUpdateTime(LocalDateTime.now());
        doctorMapper.updateById(existingDoctor);

        log.info("医生状态更新成功 - 医生ID: {}, 新状态: {}",
                doctorStatusUpdateDTO.getId(), status);
        return existingDoctor;
    }

    /**
     * 根据ID查询医生信息
     * @param id 医生ID
     * @return 医生实体
     */
    @Override
    @Cacheable(value = "doctor" ,key = "#id",unless = "#result == null")
    public Doctor getById(Long id) {
        // 检查参数是否为空
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }
        return doctorMapper.selectById(id);
    }

    /**
     * 根据科室ID获取医生列表
     * @param deptId 科室ID
     * @return 医生列表
     */
    @Override
    public List<Doctor> getDoctorsByDeptId(Long deptId) {
        // 读取时使用类型安全的方法
        List<Doctor> doctorsInDept = redisUtil.getList("doctor_dept::" + deptId, Doctor.class);

        if(doctorsInDept == null) {
            // 从数据库查询（缓存未命中时）
            QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
            wrapper.eq("dept_id", deptId);
            wrapper.eq("status", 1); // 只查询在职医生
            doctorsInDept = doctorMapper.selectList(wrapper);
        }
        return doctorsInDept;
    }

    /**
     * 医生忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "doctor", key = "#result.id"),
                    @CacheEvict(value = "doctor_dept", key = "#result.deptId")
            }
    )
    public Doctor forgotPassword(String phone, String newPassword) {
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
        Doctor doctor = doctorMapper.selectOne(new QueryWrapper<Doctor>().eq("phone", phone));
        if (doctor == null) {
            throw new BaseException(MessageConstant.PHONE_NOT_FOUND);
        }

        // 更新密码
        Doctor doctorToUpdate = new Doctor();
        doctorToUpdate.setId(doctor.getId());
        doctorToUpdate.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        doctorToUpdate.setDeptId(doctor.getDeptId());
        doctorToUpdate.setUpdateTime(LocalDateTime.now());

        int result = doctorMapper.updateById(doctorToUpdate);
        if(result <= 0){
            throw new BaseException(MessageConstant.UPDATE_FAILED);
        }

        return doctorToUpdate;
    }
}
