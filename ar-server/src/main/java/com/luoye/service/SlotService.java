package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.slot.SlotInventoryInfo;
import com.luoye.dto.slot.SlotPageQueryDTO;
import com.luoye.dto.slot.SlotReleaseDTO;
import com.luoye.entity.Slot;
import com.luoye.vo.PageResult;
import com.luoye.vo.SlotVO;

import java.time.LocalDate;
import java.util.List;

public interface SlotService extends IService<Slot> {
    /**
     * 放出号源
     * @param slotReleaseDTO 号源信息
     * @return 放号结果
     */
    Slot releaseSlot(SlotReleaseDTO slotReleaseDTO);

    /**
     * 根据医生ID和日期查询号源
     * @param doctorId 医生ID
     * @param scheduleDate 出诊日期
     * @return 号源列表
     */
    List<SlotVO> getSlotsByDoctorAndDate(Long doctorId, LocalDate scheduleDate);


    /**
     * 根据ID获取单个号源
     * @param slotId 号源ID
     * @return 号源实体
     */
    Slot getSlotById(Long slotId);

    /**
     * 删除号源并清理缓存
     * @param slotId 号源ID
     * @return 是否成功
     */
    boolean deleteSlot(Long slotId);

    /**
     * 更新号源信息并刷新缓存
     * @param slot 号源实体
     * @return 是否成功
     */
    boolean updateSlot(Slot slot);


    /**
     * 根据日期范围查询号源
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 号源列表
     */
    List<Slot> getSlotsByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 分页查询号源
     * @param slotPageQueryDTO 查询条件
     * @return 分页查询结果
     */
    PageResult<Slot> pageQueryByConditions(SlotPageQueryDTO slotPageQueryDTO);


    /**
     * 无锁版本的号源预订取消
     * @param slotId 号源ID
     * @param orderId 订单ID（用于验证）
     * @return 是否成功
     */
    boolean cancelSlot(Long slotId, Long orderId);

    /**
     * 无锁版本的号源预订
     * @param slotId 号源ID
     * @return 是否成功
     */
    boolean bookSlot(Long slotId);


    /**
     * 获取号源实时库存信息
     *
     * @param slotId 号源ID
     * @return 库存信息
     */
    SlotInventoryInfo getSlotInventoryInfo(Long slotId);

    /**
     * 检查号源是否可用
     * @param slotId 号源ID
     * @return 是否可用
     */
    boolean isSlotAvailable(Long slotId);

    /**
     * 检查患者是否已在同一时间段重复挂号
     * @param patientId 患者ID
     * @param slotId 当前要挂号的号源ID
     * @return 是否重复挂号
     */
    boolean isDuplicateRegistration(Long patientId, Long slotId);
}
