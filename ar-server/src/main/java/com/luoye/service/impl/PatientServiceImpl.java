package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.patient.PatientLoginDTO;
import com.luoye.dto.patient.PatientQueryDTO;
import com.luoye.dto.patient.PatientRegisterDTO;
import com.luoye.dto.patient.PatientUpdateDTO;
import com.luoye.entity.Patient;
import com.luoye.exception.BaseException;
import com.luoye.exception.LoginException;
import com.luoye.exception.PhoneRepetitionException;
import com.luoye.mapper.PatientMapper;
import com.luoye.service.PatientService;
import com.luoye.util.JwtUtil;
import com.luoye.util.RedisUtil;
import com.luoye.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableCaching
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    @Lazy
    private PatientService patientService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 患者注册
     * @param patientRegisterDTO 患者注册数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(PatientRegisterDTO patientRegisterDTO) {

        Patient patient = new Patient();
        BeanUtils.copyProperties(patientRegisterDTO, patient);

        // 验证必要参数
        if(patient.getPhone() == null || patient.getPassword() == null ||
                patient.getPhone().isEmpty() || patient.getPassword().isEmpty()){
            throw new BaseException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }

        //判断姓名是否为空
        if(patient.getName() == null || patient.getName().isEmpty())
            throw new BaseException(MessageConstant.NAME_NOT_NULL);

        //判断用户名称长度是否满足要求
        if(patient.getName().length() > 10 || patient.getName().length() < 2)
            throw new BaseException(MessageConstant.NAME_LENGTH_ERROR);

        //判断手机号格式是否满足要求
        if(!patient.getPhone().matches("^1\\d{10}$"))
            throw new BaseException(MessageConstant.PHONE_FORMAT_ERROR);

        //判断邮箱格式是否满足要求
        if(patient.getEmail() != null && !patient.getEmail().matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"))
            throw new BaseException(MessageConstant.EMAIL_FORMAT_ERROR);

        //判断性别是否合法
        if(patient.getGender() != 1 && patient.getGender() != 0)
            throw new BaseException(MessageConstant.GENDER_ERROR);

        //判断身份证号码格式是否满足要求
        if(patient.getCard() != null && !patient.getCard().matches("^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"))
            throw new BaseException(MessageConstant.CARD_FORMAT_ERROR);

        // 判断手机号是否已注册
        Long count = patientMapper.selectCount(new QueryWrapper<Patient>().eq("phone", patient.getPhone()));
        if(count > 0) throw new PhoneRepetitionException(MessageConstant.PHONE_ERROR);

        patient.setPassword(DigestUtils.md5DigestAsHex(patient.getPassword().getBytes()));

        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        int insert = patientMapper.insert(patient);

        if(insert != 1) throw new BaseException(MessageConstant.REGISTER_ERROR);

    }

    /**
     * 患者登录
     * @param patientLoginDTO 患者登录数据传输对象
     * @return 登录结果Map
     */
    @Override
    public Map<String, Object> login(PatientLoginDTO patientLoginDTO) {
        // 参数验证
        if (patientLoginDTO.getPhone() == null || patientLoginDTO.getPassword() == null) {
            throw new RuntimeException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }
        // 对密码进行md5加密
        patientLoginDTO.setPassword(DigestUtils.md5DigestAsHex(patientLoginDTO.getPassword().getBytes()));
        // 查询数据库
        Patient patient = patientMapper.selectOne(new QueryWrapper<Patient>()
                .eq("phone", patientLoginDTO.getPhone())
                .eq("password", patientLoginDTO.getPassword()));
        if(patient == null) throw new LoginException(MessageConstant.LOGIN_ERROR);
        patient.setPassword(null);
        //生成jwt令牌
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("patientId", patient.getId());
        claims.put("phone", patient.getPhone());
        String token = JwtUtil.createToken(claims);
        //生成token
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("patient", patient);
        //返回结果
        return result;
    }

    /**
     * 更新患者信息
     * @param patientUpdateDTO 患者更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "patient",
            key = "#patientUpdateDTO.id != null ? patientUpdateDTO.id : T(com.luoye.context.BaseContext).getCurrentId()")
    public void update(PatientUpdateDTO patientUpdateDTO) {
        // 检查传入的ID是否为空，如果为空则从BaseContext中获取
        if (patientUpdateDTO.getId() == null) {
            // 从BaseContext中获取
            // 先验证BaseContext中的类型是否为患者
            String currentUserType = BaseContext.getCurrentIdentity();
            if ("PATIENT".equals(currentUserType)) {
                Long currentPatientId = BaseContext.getCurrentId();
                patientUpdateDTO.setId(currentPatientId);
            } else {
                throw new BaseException(MessageConstant.NO_PERMISSION);
            }
        }

        // 验证ID是否存在
        if (patientUpdateDTO.getId() == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        //检查性别是否合法
        if (patientUpdateDTO.getGender() != null && patientUpdateDTO.getGender() != 1 && patientUpdateDTO.getGender() != 0) {
            throw new BaseException(MessageConstant.GENDER_ERROR);
        }

        //判断邮箱格式是否满足要求
        if(patientUpdateDTO.getEmail() != null && !patientUpdateDTO.getEmail().matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"))
            throw new BaseException(MessageConstant.EMAIL_FORMAT_ERROR);

        //判断身份证号码格式是否满足要求
        if(patientUpdateDTO.getCard() != null && !patientUpdateDTO.getCard().matches("^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"))
            throw new BaseException(MessageConstant.CARD_FORMAT_ERROR);

        // 手机号验证
        if (patientUpdateDTO.getPhone() != null) {
            if (!patientUpdateDTO.getPhone().matches("^1\\d{10}$")) {
                throw new BaseException(MessageConstant.PHONE_FORMAT_ERROR);
            }
        }

        // 检查患者是否存在
        Patient existingPatient = patientService.getById(patientUpdateDTO.getId());
        if (existingPatient == null) {
            throw new BaseException(MessageConstant.PATIENT_NOT_FOUND);
        }

        // 检查手机号是否被其他患者使用（除了当前患者之外）
        if (patientUpdateDTO.getPhone() != null) {
            QueryWrapper<Patient> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", patientUpdateDTO.getPhone());
            wrapper.ne("id", patientUpdateDTO.getId()); // 排除当前患者自身
            Long count = patientMapper.selectCount(wrapper);
            if (count > 0) {
                throw new PhoneRepetitionException(MessageConstant.PHONE_ERROR);
            }
        }

        // 复制属性到现有患者对象，排除密码和创建时间
        BeanUtils.copyProperties(patientUpdateDTO, existingPatient, "password", "createTime");

        // 设置更新时间
        existingPatient.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        patientMapper.updateById(existingPatient);
    }

    /**
     * 删除患者
     * @param id 患者ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "patient", key = "#id")
    public void delete(Long id) {
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        Patient existingPatient = patientService.getById(id);
        if (existingPatient == null) {
            throw new BaseException(MessageConstant.PATIENT_NOT_FOUND);
        }

        // 执行物理删除
        patientMapper.deleteById(id);
    }

    /**
     * 根据ID查询患者信息
     * @param id 患者ID
     * @return 患者实体
     */
    @Override
    @Cacheable(value = "patient", key = "#id", unless = "#result == null")
    public Patient getById(Long id) {
        // 验证输入参数
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        //自动处理缓存和数据库查询
        Patient patient = patientMapper.selectById(id);

        if (patient == null) {
            throw new BaseException(MessageConstant.PATIENT_NOT_FOUND);
        }

        return patient;
    }


    /**
     * 患者忘记密码
     * @param phone 手机号
     * @param newPassword 新密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "patient", key = "#result")
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
        Patient patient = patientMapper.selectOne(new QueryWrapper<Patient>().eq("phone", phone));
        if (patient == null) {
            throw new BaseException(MessageConstant.PHONE_NOT_FOUND);
        }

        // 更新密码
        Patient patientToUpdate = new Patient();
        patientToUpdate.setId(patient.getId());
        patientToUpdate.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        patientToUpdate.setUpdateTime(LocalDateTime.now());

        int result = patientMapper.updateById(patientToUpdate);
        if (result <= 0) {
            throw new RuntimeException(MessageConstant.UPDATE_FAILED);
        }

        return patient.getId();
    }

    /**
     * 分页查询患者信息
     * @param patientQueryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<Patient> pageQuery(PatientQueryDTO patientQueryDTO) {

        int page = patientQueryDTO.getPage()!= null ? patientQueryDTO.getPage() : 1;
        int pageSize = patientQueryDTO.getPageSize() != null ? patientQueryDTO.getPageSize() : 10;
        // 页码默认为1
        if (page < 1) {
            patientQueryDTO.setPage(1);
        }

        // 页大小默认为10，最大100
        if (pageSize < 1) {
            patientQueryDTO.setPageSize(10);
        } else if (pageSize > 100) {
            patientQueryDTO.setPageSize(100);
        }

        // 排序字段验证
        String sortBy = patientQueryDTO.getSortBy();
        if (sortBy != null && !sortBy.isEmpty()) {
            // 允许的排序字段白名单
            Set<String> allowedSortFields = Set.of("id", "name", "phone", "gender", "create_time", "update_time");
            if (!allowedSortFields.contains(sortBy)) {
                patientQueryDTO.setSortBy("create_time"); // 设置默认排序字段
            }
        }

        // 排序方向验证
        String sortDir = patientQueryDTO.getSortDir();
        if (sortDir != null && !sortDir.isEmpty()) {
            if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
                patientQueryDTO.setSortDir("desc"); // 设置默认排序方向
            }
        }

        // 创建分页对象
        Page<Patient> patientPage= new Page<>(page, pageSize);

        // 构建查询条件
        QueryWrapper<Patient> queryWrapper = new QueryWrapper<>();

        // 按照id查询
        if (patientQueryDTO.getId() != null) {
            queryWrapper.eq("id", patientQueryDTO.getId());
        }

        // 按照患者姓名查询(模糊匹配)
        if (patientQueryDTO.getName() != null && !patientQueryDTO.getName().isEmpty()) {
            queryWrapper.like("name", patientQueryDTO.getName());
        }

        // 按照手机号查询
        if (patientQueryDTO.getPhone() != null && !patientQueryDTO.getPhone().isEmpty()) {
            queryWrapper.eq("phone", patientQueryDTO.getPhone());
        }

        // 按照性别查询
        if (patientQueryDTO.getGender() != null) {
            queryWrapper.eq("gender", patientQueryDTO.getGender());
        }

        // 设置排序规则
        sortBy = patientQueryDTO.getSortBy();
        if (sortBy == null || sortBy.isEmpty()) {
            // 默认按照创建时间排序
            sortBy = "create_time";
        }

        // 设置排序方式
        if ("asc".equalsIgnoreCase(patientQueryDTO.getSortDir())) {
            queryWrapper.orderByAsc(sortBy);
        } else {
            queryWrapper.orderByDesc(sortBy);
        }

        // 执行查询
        Page<Patient> patientPageResult= patientMapper.selectPage(patientPage, queryWrapper);

        // 封装结果
        PageResult<Patient> pageResult = new PageResult<>();
        pageResult.setRecords(patientPageResult.getRecords());
        pageResult.setTotal(patientPageResult.getTotal());
        pageResult.setPages(Math.toIntExact(patientPageResult.getPages()));
        pageResult.setCurrent(Math.toIntExact(patientPageResult.getCurrent()));
        pageResult.setSize(Math.toIntExact(patientPageResult.getSize()));

        return pageResult;
    }

}
