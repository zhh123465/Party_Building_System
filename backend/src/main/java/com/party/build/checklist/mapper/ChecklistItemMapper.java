package com.party.build.checklist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.party.build.checklist.domain.ChecklistItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChecklistItemMapper extends BaseMapper<ChecklistItem> {
}
