package com.luoye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.slot.SlotInventoryInfo;
import com.luoye.dto.slot.SlotPageQueryDTO;
import com.luoye.dto.slot.SlotReleaseDTO;
import com.luoye.entity.*;

import com.luoye.exception.BaseException;
import com.luoye.mapper.*;
import com.luoye.service.SlotService;
import com.luoye.util.RedisUtil;
import com.luoye.vo.PageResult;
import com.luoye.vo.SlotVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@EnableCaching
@Slf4j
public class SlotServiceImpl extends ServiceImpl<SlotMapper, Slot>  implements SlotService {
    @Autowired
    private SlotMapper slotMapper;
    @Autowired
    @Lazy
    private SlotService slotService;
    @Autowired
    private DoctorMapper doctorMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 放号
     * @param slotReleaseDTO 号源信息
     * @return 是否成功
     */
    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "slot_doctor_date", key = "#result.doctorId + '::' + #result.scheduleDate"),
                    @CacheEvict(value = "slot_inventory", key = "'bookedCount::' + #result.id"),
                    @CacheEvict(value = "slot_inventory", key = "'totalCount::' + #result.id")
            },
            put = {
                    @CachePut(value = "slot", key = "#result.id", condition = "#result != null")
            }
    )
    public Slot releaseSlot(SlotReleaseDTO slotReleaseDTO) {
        // 从BaseContext获取当前用户ID和身份
        Long currentUserId = BaseContext.getCurrentId();
        String identity = BaseContext.getCurrentIdentity();
        log.info("调试信息 - 当前用户ID: {}, 身份: {}", currentUserId, identity);


        if (currentUserId == null || identity == null) {
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }

        //挂号费验证
        if (slotReleaseDTO.getFeeAmount() == null ||
                slotReleaseDTO.getFeeAmount() <= 0 ||
                slotReleaseDTO.getFeeAmount() > 10000) { // 100元 = 10000分
            throw new BaseException(MessageConstant.FEE_AMOUNT_INVALID);
        }

        // 验证权限
        hasReleasePermission(identity, currentUserId, slotReleaseDTO.getDoctorId());

        // 验证医生是否存在 先查询缓存 查询不到再查询数据库
        Doctor doctor = redisUtil.getEntityWithCache("doctor::", slotReleaseDTO.getDoctorId(), Doctor.class,
                doctorId -> doctorMapper.selectById(doctorId));
        if (doctor == null) {
            throw new BaseException(MessageConstant.DOCTOR_NOT_FOUND);
        }

        // 检查医生是否在职
        if (doctor.getStatus() != 1) {
            throw new BaseException(MessageConstant.DOCTOR_NOT_AVAILABLE);
        }

        // 获取医生所在科室（自动从医生信息中获取）
        Long deptId = doctor.getDeptId();
        if (deptId == null) {
            throw new BaseException(MessageConstant.DOCTOR_DEPT_NOT_ASSIGNED);
        }

        // 验证科室是否存在且启用
        Dept dept = redisUtil.getEntityWithCache("dept::", deptId, Dept.class,
                deptIdParam -> deptMapper.selectById(deptIdParam));
        if (dept == null) {
            throw new BaseException(MessageConstant.DEPT_NOT_FOUND);
        }
        if (dept.getStatus() != 1) {
            throw new BaseException(MessageConstant.DEPT_NOT_AVAILABLE);
        }

        // 添加号源数量验证
        if (slotReleaseDTO.getTotalCount() == null || slotReleaseDTO.getTotalCount() <= 0) {
            throw new BaseException(MessageConstant.SLOT_COUNT_INVALID);
        }
        if (slotReleaseDTO.getTotalCount() > Slot.MAX_SLOT_COUNT) { // 假设最大100个号源
            throw new BaseException(MessageConstant.SLOT_COUNT_TOO_LARGE);
        }

        // 添加日期验证
        if (slotReleaseDTO.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BaseException(MessageConstant.SLOT_DATE_INVALID);
        }

        // 检查是否已经存在相同的号源
        LambdaQueryWrapper<Slot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Slot::getDoctorId, slotReleaseDTO.getDoctorId())
                .eq(Slot::getScheduleDate, slotReleaseDTO.getScheduleDate())
                .eq(Slot::getTimePeriod, slotReleaseDTO.getTimePeriod());
        List<Slot> existingSlots = slotMapper.selectList(queryWrapper);

        if (!existingSlots.isEmpty()) {
            throw new BaseException(MessageConstant.SLOT_ALREADY_EXISTS);
        }

        // 创建号源对象
        Slot slot = new Slot();
        BeanUtils.copyProperties(slotReleaseDTO, slot);
        slot.setDeptId(deptId);
        slot.setBookedCount(0);
        slot.setStatus(Slot.STATUS_AVAILABLE); // 使用常量替代硬编码
        slot.setCreateTime(LocalDateTime.now());
        slot.setUpdateTime(LocalDateTime.now());

        // 保存号源
        int result = slotMapper.insert(slot);

        return slot;
    }


    /**
     * 根据医生ID和日期查询号源
     * @param doctorId 医生ID
     * @param scheduleDate 出诊日期
     * @return 列表
     */
    @Override
    public List<SlotVO> getSlotsByDoctorAndDate(Long doctorId, LocalDate scheduleDate) {

        // 获取医生信息
        if (doctorId == null) {
            String currentIdentity = BaseContext.getCurrentIdentity();
            Long currentId = BaseContext.getCurrentId();

            // 如果当前用户是医生，查询自己的号源
            if ("DOCTOR".equals(currentIdentity) && currentId != null) {
                doctorId = currentId;
            }
            else {
                // 如果当前用户是管理员，但没有指定医生，则可能需要抛出异常
                throw new BaseException("未指定医生ID且当前用户无权限查询所有号源");
            }
        }

        //获取日期信息
        if(scheduleDate == null){
            scheduleDate = LocalDate.now();
        }

        // 先从Redis缓存查询
        String cacheKey = "slot_doctor_date::" + doctorId + "::" + scheduleDate;
        List<SlotVO> cachedSlots = redisUtil.getList(cacheKey, SlotVO.class);
        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            return cachedSlots;
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Slot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Slot::getDoctorId, doctorId)
                .eq(Slot::getScheduleDate, scheduleDate)
                .orderByAsc(Slot::getTimePeriod);
        List<Slot> slots = slotMapper.selectList(queryWrapper);

        // 转换为VO对象
        List<SlotVO> slotVOs = slots.stream().map(this::convertToVO).collect(Collectors.toList());

        // 将结果缓存到Redis
        if (!slotVOs.isEmpty()) {
            redisUtil.set(cacheKey, slotVOs, 2, TimeUnit.HOURS);
        }

        return slotVOs;
    }

    /**
     * 将Slot实体转换为SlotVO
     * @param slot Slot实体
     * @return SlotVO
     */
    private SlotVO convertToVO(Slot slot) {
        SlotVO vo = new SlotVO();
        //批量属性复制
        BeanUtils.copyProperties(slot, vo);

        // 设置医生信息
        Doctor doctor = redisUtil.getEntityWithCache("doctor::", slot.getDoctorId(), Doctor.class,
                doctorId -> doctorMapper.selectById(doctorId));
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            vo.setDoctorPost(doctor.getPost());
            vo.setDoctorIntroduction(doctor.getIntroduction());
        }

        // 设置科室信息
        if (slot.getDeptId() != null) {
            Dept dept = redisUtil.getEntityWithCache("dept::", slot.getDeptId(), Dept.class,
                    deptId -> deptMapper.selectById(deptId));
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }

        return vo;
    }

    /**
     * 根据ID获取号源
     * @param slotId 号源ID
     * @return 号源
     */
    @Override
    @Cacheable(value = "slot", key = "#slotId", unless = "#result == null")
    public Slot getSlotById(Long slotId) {
        Slot slot = slotMapper.selectById(slotId);
        if (slot == null) {
            throw new BaseException(MessageConstant.SLOT_NOT_EXIST);
        }
        return slot;
    }

    /**
     * 根据日期范围查询号源
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 号源列表
     */
    @Override
    public List<Slot> getSlotsByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Slot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(Slot::getScheduleDate, startDate, endDate)
                .orderByAsc(Slot::getDoctorId)
                .orderByAsc(Slot::getScheduleDate)
                .orderByAsc(Slot::getTimePeriod);

        return slotMapper.selectList(queryWrapper);
    }

    /**
     * 更新号源信息并刷新缓存
     * @param slot 号源实体
     * @return 是否成功
     */
    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "slot_doctor_date", key = "#slot.doctorId + '::' + #slot.scheduleDate"),
                    @CacheEvict(value = "slot_inventory", key = "'bookedCount::' + #slot.id"),
                    @CacheEvict(value = "slot_inventory", key = "'totalCount::' + #slot.id")
            },
            put = {
                    @CachePut(value = "slot", key = "#slot.id", condition = "#result == true")
            }
    )
    public boolean updateSlot(Slot slot) {
        slot.setUpdateTime(LocalDateTime.now());
        int result = slotMapper.updateById(slot);

        return result > 0;
    }

    /**
     * 删除号源
     * @param slotId 号源ID
     * @return 删除结果
     */
    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "slot", key = "#slotId"),
                    @CacheEvict(value = "slot_doctor_date", allEntries = true),
                    @CacheEvict(value = "slot_inventory", key = "'bookedCount::' + #slotId"),
                    @CacheEvict(value = "slot_inventory", key = "'totalCount::' + #slotId")
            }
    )
    public boolean deleteSlot(Long slotId) {
        Slot slot = slotService.getById(slotId);
        // 判断号源是否存在
        if(slot == null){
            throw new BaseException(MessageConstant.SLOT_NOT_EXIST);
        }
        //判断号源是否有人预约
        if(slot.getBookedCount() >= 1){
            throw new BaseException(MessageConstant.SLOT_HAS_BOOKING);
        }
        //只能将号源改为已停诊
        Slot updateSlot = new Slot();
        updateSlot.setId(slotId);
        updateSlot.setStatus(Slot.STATUS_STOPPED);
        updateSlot.setUpdateTime(LocalDateTime.now());

        int result = slotMapper.updateById(updateSlot);

        return result > 0;
    }

    /**
     * 验证放号权限
     * @param identity 操作员身份 (ADMIN 或 DOCTOR)
     * @param currentUserId 操作员ID
     * @param doctorId 目标医生ID
     */
    private void hasReleasePermission(String identity, Long currentUserId, Long doctorId) {

        if ("ADMIN".equals(identity)) {
            // 管理员有权限放出任意号源
            Admin admin = redisUtil.getEntityWithCache("admin:", currentUserId, Admin.class,
                    adminId -> adminMapper.selectById(adminId));
            if (admin == null) {
                throw new BaseException(MessageConstant.ADMIN_NOT_EXIST);
            }
        } else if ("DOCTOR".equals(identity)) {
            // 医生只能放出自己的号源，且必须是科室主任
            Doctor doctor = redisUtil.getEntityWithCache("doctor:", currentUserId, Doctor.class,
                    doctorIdParam -> doctorMapper.selectById(doctorId));
            if (doctor == null) {
                throw new BaseException(MessageConstant.DOCTOR_NOT_FOUND);
            }
            if (!doctor.getId().equals(doctorId)) {
                throw new BaseException(MessageConstant.DOCTOR_ONLY_SELF_SLOT);
            }
            if (doctor.getPost() == null || doctor.getPost() != 1) { // 1表示科室主任
                throw new BaseException(MessageConstant.DIRECTOR_ONLY_RELEASE_SLOT);
            }
        } else {
            throw new BaseException(MessageConstant.NO_PERMISSION);
        }
    }

    /**
     * 根据条件分页查询号源
     * @param slotPageQueryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<Slot> pageQueryByConditions(SlotPageQueryDTO slotPageQueryDTO) {
        // 获取当前用户ID和身份
        Long currentUserId = BaseContext.getCurrentId();
        String identity = BaseContext.getCurrentIdentity();

        // 创建分页对象
        Page<Slot> page = new Page<>(slotPageQueryDTO.getPage(), slotPageQueryDTO.getSize());

        //构建查询条件
        QueryWrapper<Slot> queryWrapper = new QueryWrapper<>();

        //进行权限控制,如果是医生就只能查询自己的号源，如果是管理员，可以查询所有号源
        if("DOCTOR".equals(identity)){
            // 医生只能查询自己的号源
            queryWrapper.eq("doctor_id",currentUserId);
        } else if ("ADMIN".equals(identity)) {
            // 管理员可以查询所有号源，但如果有条件限制，则按条件查询
            if(slotPageQueryDTO.getDoctorId() != null){
                queryWrapper.eq("doctor_id",slotPageQueryDTO.getDoctorId());
            } else if(slotPageQueryDTO.getDoctorName() != null && !slotPageQueryDTO.getDoctorName().isEmpty()){
                // 根据医生姓名查询医生ID，然后在号源表中查询
                LambdaQueryWrapper<Doctor> doctorQueryWrapper = new LambdaQueryWrapper<>();
                doctorQueryWrapper.like(Doctor::getName, slotPageQueryDTO.getDoctorName());
                List<Doctor> doctors = doctorMapper.selectList(doctorQueryWrapper);

                if (!doctors.isEmpty()) {
                    // 提取医生ID列表
                    List<Long> doctorIds = doctors.stream()
                            .map(Doctor::getId)
                            .collect(java.util.stream.Collectors.toList());

                    // 在号源表中按医生ID列表查询
                    queryWrapper.in("doctor_id", doctorIds);
                } else {
                    // 如果没有找到匹配的医生，返回空结果
                    PageResult<Slot> pageResult = new PageResult<>();
                    pageResult.setTotal(0L);
                    pageResult.setRecords(java.util.Collections.emptyList());
                    pageResult.setPages(0);
                    pageResult.setCurrent(0);
                    pageResult.setSize(0);
                    return pageResult;
                }
            }
        }

        // 按照日期范围查询
        if(slotPageQueryDTO.getStartDate() != null && slotPageQueryDTO.getEndDate() != null){
            queryWrapper.between("schedule_date",slotPageQueryDTO.getStartDate(),slotPageQueryDTO.getEndDate());
        }else if (slotPageQueryDTO.getStartDate() != null) {
            queryWrapper.ge("schedule_date", slotPageQueryDTO.getStartDate());
        } else if (slotPageQueryDTO.getEndDate() != null) {
            queryWrapper.le("schedule_date", slotPageQueryDTO.getEndDate());
        }

        if (slotPageQueryDTO.getTimePeriod() != null && !slotPageQueryDTO.getTimePeriod().isEmpty()) {
            queryWrapper.eq("time_period", slotPageQueryDTO.getTimePeriod());
        }

        if (slotPageQueryDTO.getStatus() != null) {
            queryWrapper.eq("status", slotPageQueryDTO.getStatus());
        }

        // 按出诊日期和时间段排序
        queryWrapper.orderByDesc("schedule_date").orderByAsc("time_period");

        // 执行分页查询
        Page<Slot> resultPage = slotMapper.selectPage(page, queryWrapper);

        // 封装返回结果
        PageResult<Slot> pageResult = new PageResult<>();
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setRecords(resultPage.getRecords());
        pageResult.setPages((int) resultPage.getPages());
        pageResult.setCurrent((int) resultPage.getCurrent());
        pageResult.setSize((int) resultPage.getSize());

        return pageResult;
    }



    /**
     * 号源预订取消
     */
    @Override
    @Transactional
    @CachePut(value = "slot", key = "#slotId", condition = "#result == true")
    public boolean cancelSlot(Long slotId, Long orderId) {
        try {
            //获取当前号源信息
            Slot slot = slotService.getSlotById(slotId);
            if (slot == null) {
                return false;
            }

            // 检查并更新Redis库存
            String bookedCountKey = "slot_inventory::bookedCount::" + slotId ;
            Long newBookedCount = redisUtil.decrementDirect(bookedCountKey);

            // 更新数据库
            Slot updateSlot = new Slot();
            updateSlot.setId(slotId);
            updateSlot.setBookedCount(newBookedCount.intValue());
            updateSlot.setUpdateTime(LocalDateTime.now());

            // 如果之前是已约满状态，恢复为可用
            if (Objects.equals(slot.getStatus(), Slot.STATUS_FULL)) {
                updateSlot.setStatus(Slot.STATUS_AVAILABLE);
            }

            int updateResult = slotMapper.updateById(updateSlot);

            if (updateResult > 0) {
                return true;
            } else {
                log.error("数据库更新失败，slotId: {}" + slotId);
                // 回滚Redis操作
                redisUtil.incrementDirect(bookedCountKey);
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 号源预订
     */
    @Override
    @Transactional
    @CachePut(value = "slot", key = "#slotId", condition = "#result == true")
    public boolean bookSlot(Long slotId) {
        try {
            // 一步到位：获取号源并直接预订
            Slot slot = slotService.getSlotById(slotId);
            if (slot == null || !Objects.equals(slot.getStatus(), Slot.STATUS_AVAILABLE)) {
                return false;
            }

            //获取号源库存
            String bookedCountKey = "slot_inventory::bookedCount::" + slotId ;
            String totalCountKey = "slot_inventory::totalCount::" + slotId ;
            Integer bookedCount = redisUtil.get(bookedCountKey,Integer.class);
            Integer totalCount  = redisUtil.get(totalCountKey,Integer.class);

            return executeBooking(slotId, bookedCount, totalCount);

        } catch (Exception e) {
            log.error("预订号源失败，slotId: " + slotId, e);
            return false;
        }
    }

    /**
     * 尝试执行预订
     * @param slotId 号源ID
     * @param currentBooked 当前已预订数量
     * @param totalCount 总数量
     * @return 是否成功
     */
    private boolean executeBooking(Long slotId, Integer currentBooked, Integer totalCount) {
        // 更新Redis库存
        String bookedCountKey = "slot_inventory::bookedCount::" + slotId ;
        Long newBooked = redisUtil.incrementDirect(bookedCountKey);

        if (newBooked != null && newBooked <= totalCount) {
            // 一次性更新数据库和缓存
            Slot updateSlot = new Slot();
            updateSlot.setId(slotId);
            updateSlot.setBookedCount(newBooked.intValue());
            updateSlot.setStatus(newBooked >= totalCount ? Slot.STATUS_FULL : Slot.STATUS_AVAILABLE);
            updateSlot.setUpdateTime(LocalDateTime.now());

            if (slotMapper.updateById(updateSlot) > 0) {
                return true;
            }
            // 失败回滚
            redisUtil.decrementDirect(bookedCountKey);
        }
        return false;
    }

    /**
     * 获取号源实时库存信息
     * @param slotId 号源ID
     * @return 库存信息
     */
    @Override
    public SlotInventoryInfo getSlotInventoryInfo(Long slotId) {
        // 直接获取号源信息，避免重复查询
        Slot slot = slotService.getSlotById(slotId);
        if (slot == null) {
            return null;
        }
        // 从缓存获取已预订数量
        String bookedCountKey = "slot_inventory::bookedCount::" + slotId;
        Integer bookedCount = redisUtil.get(bookedCountKey, Integer.class);
        if (bookedCount == null) {
            bookedCount = slotMapper.selectById(slotId).getBookedCount();
        }

        // 从缓存获取总数
        String totalCountKey = "slot_inventory::totalCount::" + slotId;
        Integer totalCount = redisUtil.get(totalCountKey, Integer.class);
        if (totalCount == null) {
            totalCount = slotMapper.selectById(slotId).getTotalCount();
        }

        // 构建库存信息
        SlotInventoryInfo inventoryInfo = new SlotInventoryInfo();
        inventoryInfo.setSlotId(slotId);
        inventoryInfo.setTotalCount(totalCount);
        inventoryInfo.setBookedCount(bookedCount);
        inventoryInfo.setRemainingCount(totalCount - bookedCount);
        inventoryInfo.setStatus(slot.getStatus());

        return inventoryInfo;
    }

    /**
     * 检查号源是否可用
     * @param slotId 号源ID
     * @return 是否可用
     */
    @Override
    public boolean isSlotAvailable(Long slotId) {
        // 直接检查号源状态，避免重复查询
        Slot slot = slotService.getSlotById(slotId);
        if (slot == null || slot.getStatus() != Slot.STATUS_AVAILABLE) {
            return false;
        }

        // 从缓存获取库存信息
        String bookedCountKey = "slot_inventory::bookedCount::" + slotId;
        String totalCountKey = "slot_inventory::totalCount::" + slotId;

        Integer currentBooked = redisUtil.get(bookedCountKey, Integer.class);
        Integer totalCount = redisUtil.get(totalCountKey, Integer.class);

        if (currentBooked == null)
            currentBooked = slotMapper.selectById(slotId).getBookedCount();
        if (totalCount == null) totalCount = slotMapper.selectById(slotId).getTotalCount();

        return currentBooked < totalCount;

    }

    /**
     * 检查患者是否已在同一时间段重复挂号
     * @param patientId 患者ID
     * @param slotId 当前要挂号的号源ID
     * @return 是否重复挂号
     */
    @Override
    public boolean isDuplicateRegistration(Long patientId, Long slotId) {
        // 获取当前号源的详细信息
        Slot currentSlot = slotService.getSlotById(slotId);
        if (currentSlot == null) {
            return false;
        }

        // 查找该患者的其他订单，判断是否在同一时间段
        QueryWrapper<Order> orderQuery = new QueryWrapper<>();
        orderQuery.eq("patient_id", patientId)
                .in("order_status", Order.ORDER_STATUS_PENDING, Order.ORDER_STATUS_PAID); // 待支付和已支付的订单都算作有效预约

        // 获取该患者的所有有效订单
        List<Order> existingOrders = orderMapper.selectList(orderQuery);

        for (Order existingOrder : existingOrders) {
            // 获取现有订单对应的号源信息
            Slot existingSlot = slotService.getSlotById(existingOrder.getSlotId());
            if (existingSlot != null) {
                // 检查是否在同一日期和时间段
                if (existingSlot.getScheduleDate().equals(currentSlot.getScheduleDate()) &&
                        existingSlot.getTimePeriod().equals(currentSlot.getTimePeriod())) {
                    return true; // 发现重复挂号
                }
            }
        }

        return false;
    }
}
