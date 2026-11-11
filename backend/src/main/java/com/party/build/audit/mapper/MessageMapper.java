package com.party.build.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.party.build.audit.domain.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}