package com.party.build.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.party.build.audit.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
