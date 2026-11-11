package com.party.build.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.party.build.auth.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}