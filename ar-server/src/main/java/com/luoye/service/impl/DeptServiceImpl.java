package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.dto.dept.DeptCreateDTO;
import com.luoye.dto.dept.DeptPageQueryDTO;
import com.luoye.dto.dept.DeptStatusUpdateDTO;
import com.luoye.dto.dept.DeptUpdateDTO;
import com.luoye.entity.Dept;
import com.luoye.exception.BaseException;
import com.luoye.mapper.DeptMapper;
import com.luoye.service.DeptService;
import com.luoye.util.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.luoye.constant.MessageConstant;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@EnableCaching
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    @Lazy
    private DeptService deptService;

    @Autowired
    private RedisUtil redisUtil;


    /**
     * 分页查询科室信息
     * @param deptPageQueryDTO 科室分页查询数据传输对象
     * @return 查询结果Map
     */
    @Override
    public Map<String, Object> pageQuery(DeptPageQueryDTO deptPageQueryDTO) {

        // 设置默认页码和页面大小
        int pageNum = deptPageQueryDTO.getPage() != null ? deptPageQueryDTO.getPage() : 1;
        int pageSize = deptPageQueryDTO.getPageSize() != null ? deptPageQueryDTO.getPageSize() : 10;

        //页码默认为1
        if (pageNum < 1) {
            throw new BaseException(MessageConstant.PAGE_NUMBER_INVALID);
        }

        //页大小默认为10
        if (pageSize < 1) {
            throw new BaseException(MessageConstant.PAGE_SIZE_INVALID);
        }

        //页大小最大100
        if (pageSize > 100) {
            throw new BaseException(MessageConstant.PAGE_SIZE_TOO_LARGE);
        }

        // 排序字段验证
        String sortBy = deptPageQueryDTO.getSortBy();
        if (sortBy != null && !sortBy.isEmpty()) {
            // 允许的排序字段白名单
            Set<String> allowedSortFields = Set.of("id", "name", "type", "status", "create_time", "update_time");
            // 如果不在白名单中，则抛出异常
            if (!allowedSortFields.contains(sortBy)) {
                throw new BaseException(MessageConstant.SORT_FIELD_INVALID);
            }
        }

        // 排序方向验证
        String sortDir = deptPageQueryDTO.getSortDir();
        if (sortDir != null && !sortDir.isEmpty()) {
            if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
                throw new BaseException(MessageConstant.SORT_DIRECTION_INVALID);
            }
        }
        // 创建分页对象
        Page<Dept> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        QueryWrapper<Dept> deptQueryWrapper = new QueryWrapper<>();

        // 按照id查询
        if (deptPageQueryDTO.getId() != null) {
            deptQueryWrapper.eq("id", deptPageQueryDTO.getId());
        }

        // 按照科室名称查询(模糊匹配)
        if (deptPageQueryDTO.getName() != null && !deptPageQueryDTO.getName().isEmpty()) {
            deptQueryWrapper.like("name", deptPageQueryDTO.getName());
        }

        // 按照科室类型查询
        if (deptPageQueryDTO.getType() != null) {
            deptQueryWrapper.eq("type", deptPageQueryDTO.getType());
        }

        // 按照科室状态查询
        if (deptPageQueryDTO.getStatus() != null) {
            deptQueryWrapper.eq("status", deptPageQueryDTO.getStatus());
        }

        // 设置排序规则
        sortBy = deptPageQueryDTO.getSortBy();
        if (sortBy == null || sortBy.isEmpty()) {
            // 默认按照创建时间排序
            sortBy = "create_time";
        }

        // 设置排序方式
        if ("asc".equalsIgnoreCase(deptPageQueryDTO.getSortDir())) {
            deptQueryWrapper.orderByAsc(sortBy);
        } else {
            deptQueryWrapper.orderByDesc(sortBy);
        }

        // 执行查询
        Page<Dept> deptPage = deptMapper.selectPage(page, deptQueryWrapper);

        // 封装结果 - 确保类型一致性
        HashMap<String, Object> resultPage = new HashMap<>();
        resultPage.put("total", deptPage.getTotal());
        resultPage.put("depts", deptPage.getRecords());
        resultPage.put("pages", Math.toIntExact(deptPage.getPages())); // 转换为int
        resultPage.put("current", Math.toIntExact(deptPage.getCurrent())); // 转换为int
        resultPage.put("size", Math.toIntExact(deptPage.getSize())); // 转换为int


        return resultPage;
    }

    /**
     * 更新科室信息
     *
     * @param deptUpdateDTO 科室更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "dept", key = "#deptUpdateDTO.id"),
                    @CacheEvict(value = "dept_list", key = "'enabled'")
            }
    )
    public void update(DeptUpdateDTO deptUpdateDTO) {
        // 验证参数是否为空
        if (deptUpdateDTO.getId() == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        // 根据ID查询现有的科室信息
        Dept existingDept = deptMapper.selectById(deptUpdateDTO.getId());
        if (existingDept == null) {
            throw new BaseException(MessageConstant.DEPT_NOT_FOUND);
        }

        // 验证科室名称
        if (deptUpdateDTO.getName() != null) {
            String newName = deptUpdateDTO.getName().trim();
            // 检查名称是否为空
            if (newName.isEmpty()) {
                throw new BaseException(MessageConstant.DEPT_NAME_EMPTY);
            }

            // 检查名称长度
            if (newName.length() > 50) {
                throw new BaseException(MessageConstant.DEPT_NAME_TOO_LONG);
            }

            // 检查名称是否与其他科室重复（排除自己）
            if (!newName.equals(existingDept.getName())) {
                QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name", newName);
                queryWrapper.ne("id", existingDept.getId()); // 排除当前科室
                Dept duplicateDept = deptMapper.selectOne(queryWrapper);
                if (duplicateDept != null) {
                    throw new BaseException(newName + MessageConstant.ALREADY_EXISTS);
                }
            }

            // 设置新的科室名称
            existingDept.setName(newName);
        }

        // 验证科室类型
        if (deptUpdateDTO.getType() == null) {
            throw new BaseException(MessageConstant.DEPT_TYPE_EMPTY);
        }

        if (deptUpdateDTO.getType() < 0 || deptUpdateDTO.getType() > 1) {
            throw new BaseException(MessageConstant.DEPT_TYPE_INVALID);
        }
        existingDept.setType(deptUpdateDTO.getType());

        // 复制属性到现有科室对象，排除创建时间和更新时间
        BeanUtils.copyProperties(deptUpdateDTO, existingDept, "createTime");

        // 设置更新时间
        existingDept.setUpdateTime(LocalDateTime.now());

        // 更新数据库
        deptMapper.updateById(existingDept);
    }

    /**
     * 删除科室
     * @param id 科室ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "dept", key = "#id"),
                    @CacheEvict(value = "dept_list", key = "'enabled'")
            }
    )
    public void delete(Long id) {

        // 验证参数是否为空
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        // 根据ID查询现有的科室信息
        Dept existingDept = deptService.getById(id);
        if (existingDept == null) {
            throw new BaseException(MessageConstant.DEPT_NOT_FOUND);
        }

        // 执行删除
        deptMapper.deleteById(id);
    }

    /**
     * 更新科室状态
     * @param statusUpdateDTO 科室状态更新数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(value = "dept", key = "#statusUpdateDTO.id"),
                    @CacheEvict(value = "dept_list", key = "'enabled'")
            }
    )
    public void updateStatus(DeptStatusUpdateDTO statusUpdateDTO) {

        // 参数验证 - 确保ID和状态不为null
        if (statusUpdateDTO.getId() == null || statusUpdateDTO.getStatus() == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }


        Long id = statusUpdateDTO.getId();
        Integer status = statusUpdateDTO.getStatus();

        // 状态验证 - 确保状态为0或1
        if (!(status.equals(0) || status.equals(1))) {
            throw new BaseException(MessageConstant.STATUS_INVALID);
        }

        // 根据ID查询科室信息，确保科室存在
        Dept existingDept = deptService.getById(id);
        if (existingDept == null) {
            throw new BaseException(MessageConstant.DEPT_NOT_FOUND);
        }

        // 更新科室状态和更新时间
        existingDept.setStatus(status);
        existingDept.setUpdateTime(LocalDateTime.now());

        // 将更新后的数据保存到数据库
        deptMapper.updateById(existingDept);

    }

    /**
     * 根据ID查询科室信息
     * @param id 科室ID
     * @return 科室实体
     */
    @Override
    @Cacheable(value = "dept",key ="#id",unless = "#result == null")
    public Dept getById(Long id) {
        // 验证参数是否为空
        if (id == null) {
            throw new BaseException(MessageConstant.PARAMETER_EMPTY);
        }

        return deptMapper.selectById(id);
    }

    /**
     * 获取所有启用的科室信息
     * @return 科室列表
     */
    @Override
    public List<Dept> getAllEnabledDepts() {
        return deptMapper.selectList(new QueryWrapper<Dept>().eq("status", 1));
    }

    /**
     * 创建科室
     * @param deptCreateDTO 科室创建数据传输对象
     * @return 科室实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dept_list",allEntries = true)
    public Dept create(DeptCreateDTO deptCreateDTO) {

        //科室名称不能为空
        if (deptCreateDTO.getName() == null || deptCreateDTO.getName().trim().isEmpty()) {
            throw new BaseException(MessageConstant.DEPT_NAME_EMPTY);
        }
        //科室名称长度不能超过50个字符
        if (deptCreateDTO.getName().trim().length() > 50) {
            throw new BaseException(MessageConstant.DEPT_NAME_TOO_LONG);
        }

        //科室类型不能为空
        if (deptCreateDTO.getType() == null) {
            throw new BaseException(MessageConstant.DEPT_TYPE_EMPTY);
        }


        //科室类型必须为0(临床)或1(医技)
        if (deptCreateDTO.getType() < 0 || deptCreateDTO.getType() > 1) {
            throw new BaseException(MessageConstant.DEPT_TYPE_INVALID);
        }
        // 科室位置长度不能超过100个字符
        if (deptCreateDTO.getLocation() != null && deptCreateDTO.getLocation().length() > 100) {
            throw new BaseException(MessageConstant.DEPT_LOCATION_TOO_LONG);
        }
        // 科室描述长度不能超过500个字符
        if (deptCreateDTO.getDescription() != null && deptCreateDTO.getDescription().length() > 500) {
            throw new BaseException(MessageConstant.DEPT_DESCRIPTION_TOO_LONG);
        }

        // 检查科室名称是否已存在
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", deptCreateDTO.getName());
        Dept existingDept = deptMapper.selectOne(queryWrapper);
        if (existingDept != null) {
            throw new BaseException(MessageConstant.ALREADY_EXISTS);
        }

        // 创建新的科室实体
        Dept dept = new Dept();
        dept.setName(deptCreateDTO.getName());
        dept.setType(deptCreateDTO.getType());
        dept.setLocation(deptCreateDTO.getLocation());
        dept.setStatus(deptCreateDTO.getStatus());
        dept.setDescription(deptCreateDTO.getDescription());
        dept.setDirectorId(deptCreateDTO.getDirectorId());

        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        dept.setCreateTime(now);
        dept.setUpdateTime(now);

        // 保存到数据库
        deptMapper.insert(dept);

        return dept;
    }

    /**
     * 获取所有科室ID和名称
     * @return 科室ID和名称列表
     */
    @Override
    public List<Map<String, Object>> getAllDeptIdAndName() {
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name"); // 只查询id和name字段
        queryWrapper.orderByAsc("id");

        List<Dept> depts = deptMapper.selectList(queryWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Dept dept : depts) {
            Map<String, Object> deptInfo = new HashMap<>();
            deptInfo.put("id", dept.getId());
            deptInfo.put("name", dept.getName());
            result.add(deptInfo);
        }

        return result;
    }

}
