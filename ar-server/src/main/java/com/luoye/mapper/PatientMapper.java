package com.luoye.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luoye.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

}
