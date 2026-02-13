package com.luoye.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.luoye.dto.patient.PatientQueryDTO;
import com.luoye.entity.*;
import com.luoye.mapper.AdminMapper;
import com.luoye.mapper.DeptMapper;
import com.luoye.mapper.DoctorMapper;
import com.luoye.mapper.SlotMapper;
import com.luoye.service.*;
import com.luoye.util.RedisUtil;
import com.luoye.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CachePreloadTask {


    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private LogService logService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private SlotMapper slotMapper;

    /**
     * 每天零点执行，预加载科室和医生数据到Redis
     */
    @Scheduled(cron ="0 0 0 * * ?") // 每天零点执行
    public void preloadCacheData(){
        String operationDetail = "";
        int successFlag = 1; // 默认成功
        String errorMessage = null;
        try {

            log.info("开始执行缓存预加载任务...");
            operationDetail = "开始执行缓存预加载任务，预加载科室和医生数据到Redis";

            // 预加载所有启用的科室数据
            preloadDeptData();

            // 预加载医生数据（按科室分组）
            preloadDoctorData();

            // 预加载号源数据
            preloadSlots();

            // 预加载管理员数据
            preloadAdminData();

            // 预加载患者数据
            preloadPatientData();

            log.info("缓存预加载任务完成！");
            operationDetail += "；缓存预加载任务完成！";

        }catch (Exception e){
            successFlag = 0; // 标记为失败
            errorMessage = e.getMessage();
            operationDetail += "；缓存预加载任务执行失败: " + e.getMessage();
            log.error("缓存预加载任务执行失败: " + e.getMessage(), e);
            e.printStackTrace();
        }finally {
            // 记录操作日志
            Log logEntity = new Log();
            logEntity.setLogType("SYSTEM_TASK"); // 系统任务
            logEntity.setOperatorType("SYSTEM"); // 系统操作
            logEntity.setOperatorId(-1L); // 系统标识
            logEntity.setOperatorName("System Task"); // 系统任务名称
            logEntity.setTargetType("CACHE"); // 目标类型为缓存
            logEntity.setTargetId(null); // 没有特定的目标ID
            logEntity.setOperationDetail(operationDetail);
            logEntity.setIpAddress("SYSTEM"); // 系统任务IP
            logEntity.setSuccessFlag(successFlag);
            if (errorMessage != null) {
                logEntity.setErrorMessage(errorMessage);
            }
            logEntity.setCreateTime(LocalDateTime.now());

            logService.save(logEntity);
        }

    }


    /**
     * 预加载管理员数据
     */
    private void preloadAdminData() {
        try {
            log.info("开始预加载管理员数据...");

            // 获取所有管理员
            List<Admin> admins = adminMapper.selectList(new QueryWrapper<>());

            if (admins != null && !admins.isEmpty()) {
                Random random = new Random();
                long expireHours = 20 + random.nextInt(11); // 20-30小时


                // 缓存单个管理员信息
                for (Admin admin : admins) {
                    String adminKey = "admin::" + admin.getId();
                    redisUtil.set(adminKey, admin, expireHours, TimeUnit.HOURS);
                }

                log.info("管理员数据预加载完成，共加载 {} 个管理员", admins.size());
            } else {
                log.info("未找到管理员数据");
            }

        } catch (Exception e) {
            log.error("预加载管理员数据失败", e);
        }
    }

    /**
     * 预加载患者数据
     */
    private void preloadPatientData() {
        try {
            log.info("开始预加载患者数据...");

            // 分批加载患者数据（避免一次性加载过多数据）
            int pageSize = 1000;
            int currentPage = 1;
            int totalPatients = 0;

            while (true) {
                // 获取当前页患者数据
                PatientQueryDTO queryDTO = new PatientQueryDTO();
                queryDTO.setPage(currentPage);
                queryDTO.setPageSize(pageSize);
                PageResult<Patient> pageResult = patientService.pageQuery(queryDTO);
                List<Patient> patients = pageResult.getRecords();


                if (patients == null || patients.isEmpty()) {
                    break;
                }

                // 缓存患者信息
                for (Patient patient : patients) {
                    String patientKey = "patient::" + patient.getId();
                    redisUtil.set(patientKey, patient, 25, TimeUnit.HOURS);
                }

                totalPatients += patients.size();

                // 如果当前页是最后一页，跳出循环
                if (pageResult.getCurrent() >= pageResult.getPages()) {
                    break;
                }


                currentPage++;
            }

            log.info("患者数据预加载完成，共加载 {} 个患者", totalPatients);

        } catch (Exception e) {
            log.error("预加载患者数据失败", e);
        }
    }


    /**
     * 预加载科室数据
     */
    private void preloadDeptData() {
        // 直接从数据库获取所有启用的科室，绕过缓存
        List<Dept> enabledDepts = deptMapper.selectList(new QueryWrapper<Dept>().eq("status", 1));

        // 将所有启用的科室列表存入缓存
        String allEnabledDeptsKey = "dept_list::enabled";
        redisUtil.set(allEnabledDeptsKey, enabledDepts, 25, TimeUnit.HOURS);  // 24小时过期

        // 将每个科室单独存入缓存
        for (Dept dept : enabledDepts) {
            String deptKey = "dept::" + dept.getId();
            redisUtil.set(deptKey, dept, 25, TimeUnit.HOURS);  // 24小时过期
        }

        log.info("科室数据预加载完成，共加载 {} 个科室", enabledDepts.size());
    }

    /**
     * 预加载医生数据 - 恢复原来的科室分组缓存逻辑
     */
    private void preloadDoctorData() {
        // 直接从数据库获取所有启用的科室，绕过缓存
        List<Dept> enabledDepts = deptMapper.selectList(new QueryWrapper<Dept>().eq("status", 1));

        int totalDoctors = 0;
        for (Dept dept : enabledDepts) {
            // 根据科室ID获取该科室下的所有在职医生
            List<Doctor> doctorsInDept = doctorMapper.selectList(new QueryWrapper<Doctor>().eq("dept_id", dept.getId()).eq("status", 1));

            if (doctorsInDept != null && !doctorsInDept.isEmpty()) {
                // 将该科室下的医生列表存入缓存
                String doctorsByDeptKey = "doctor_dept::" + dept.getId();
                redisUtil.set(doctorsByDeptKey, doctorsInDept, 25, TimeUnit.HOURS);  // 24小时过期
                totalDoctors += doctorsInDept.size();
            }
        }
        //缓存医生信息
        List<Doctor> doctors = doctorMapper.selectList(new QueryWrapper<>());
        doctors.forEach(doctor -> {
            String doctorKey = "doctor::" + doctor.getId();
            redisUtil.set(doctorKey, doctor, 25, TimeUnit.HOURS);
        });

        log.info("医生数据预加载完成，共加载 {} 个医生", totalDoctors);
    }


    /**
     * 预加载号源数据
     * 缓存未来7天的号源数据，提高查询性能
     */
    private void preloadSlots() {
        try {
            log.info("开始预加载号源数据...");

            // 获取未来7天的号源数据 - 直接使用 getSlotsByDateRange 方法
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(7);
            List<Slot> upcomingSlots = slotMapper.selectList(new QueryWrapper<Slot>()
                    .ge("schedule_date", today).le("schedule_date", endDate));

            if (upcomingSlots == null || upcomingSlots.isEmpty()) {
                log.info("未找到未来7天的号源数据");
                return;
            }

            // 按医生ID分组，便于批量处理
            Map<Long, List<Slot>> slotsByDoctor = upcomingSlots.stream()
                    .collect(Collectors.groupingBy(Slot::getDoctorId));

            int totalCached = 0;
            int doctorCount = 0;

            // 逐个医生处理号源缓存
            for (Map.Entry<Long, List<Slot>> entry : slotsByDoctor.entrySet()) {
                Long doctorId = entry.getKey();
                List<Slot> doctorSlots = entry.getValue();

                // 按日期分组该医生的号源
                Map<LocalDate, List<Slot>> slotsByDate = doctorSlots.stream()
                        .collect(Collectors.groupingBy(Slot::getScheduleDate));

                // 缓存每个日期的号源列表
                for (Map.Entry<LocalDate, List<Slot>> dateEntry : slotsByDate.entrySet()) {
                    LocalDate scheduleDate = dateEntry.getKey();
                    List<Slot> dailySlots = dateEntry.getValue();

                    String cacheKey = "slot_doctor_date::" + doctorId + "::" + scheduleDate;
                    redisUtil.set(cacheKey, dailySlots, 25, TimeUnit.HOURS);
                    totalCached += dailySlots.size();
                }

                // 缓存单个号源
                for (Slot slot : doctorSlots) {
                    String slotKey = "slot::" + slot.getId();
                    redisUtil.set(slotKey, slot, 25, TimeUnit.HOURS);

                    // 同时缓存号源库存信息到slot_inventory分区
                    String bookedCountKey = "slot_inventory::bookedCount::" + slot.getId();
                    String totalCountKey = "slot_inventory::totalCount::" + slot.getId();

                    redisUtil.set(bookedCountKey, slot.getBookedCount(), 25, TimeUnit.HOURS);
                    redisUtil.set(totalCountKey, slot.getTotalCount(), 25, TimeUnit.HOURS);

                }

                doctorCount++;
            }

            log.info("号源数据预加载完成，共缓存 {} 个号源，涉及 {} 位医生",
                    totalCached, doctorCount);

        } catch (Exception e) {
            log.error("预加载号源数据失败", e);
        }
    }
    /**
     * 应用启动时立即执行一次缓存预热
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        try {
            log.info("应用启动，开始缓存预热...");

            // 检查Redis连接状态
            boolean redisConnected = redisUtil.isConnected();
            String redisStatus = redisUtil.getConnectionStatus();
            log.info("Redis连接状态: {}", redisStatus);

            // 执行完整的预加载
            preloadCacheData();
            log.info("应用启动缓存预热完成");
            if (!redisConnected) {
                log.warn("⚠️ 注意：Redis连接不可用，数据仅从数据库加载，未缓存到Redis");
            }
        } catch (Exception e) {
            log.error("应用启动缓存预热失败", e);
        }
    }
}
