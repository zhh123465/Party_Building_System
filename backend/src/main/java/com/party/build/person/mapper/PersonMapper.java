package com.party.build.person.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.party.build.person.domain.Person;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PersonMapper extends BaseMapper<Person> {
}