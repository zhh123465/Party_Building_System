package com.party.build.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.person.service.PersonService;
import com.party.build.warning.domain.TimeRule;
import com.party.build.warning.domain.Warning;
import com.party.build.warning.dto.*;
import com.party.build.warning.mapper.TimeRuleMapper;
import com.party.build.warning.mapper.WarningMapper;
import com.party.build.audit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WarningService {
    private final WarningMapper warningMapper;
    private final TimeRuleMapper timeRuleMapper;
    private final PersonMapper personMapper;
    private final PersonService personService;
    private final SysUserMapper userMapper;
    private final MessageService messageService;

    public List<TimeRule> timeRules() {
        return timeRuleMapper.selectList(new LambdaQueryWrapper<TimeRule>()
                .orderByAsc(TimeRule::getNodeCode));
    }

    @Transactional
    public TimeRule updateTimeRule(Long id, TimeRuleRequest request) {
        TimeRule rule = timeRuleMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "时限规则不存在");
        }
        if (request.getNodeName() != null) rule.setNodeName(request.getNodeName());
        if (request.getStage() != null) rule.setStage(request.getStage());
        if (request.getOffsetValue() != null) rule.setOffsetValue(request.getOffsetValue());
        if (request.getOffsetUnit() != null) rule.setOffsetUnit(request.getOffsetUnit());
        if (request.getResponsibleRole() != null) rule.setResponsibleRole(request.getResponsibleRole());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
        if (request.getWarnOffsets() != null) {
            try {
                rule.setWarnOffsets(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(request.getWarnOffsets()));
            } catch (Exception e) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "预警提前量格式错误");
            }
        }
        timeRuleMapper.updateById(rule);
        return rule;
    }

    public PageResult<WarningVO> list(long page, long size, String status, String level,
                                      Long branchId, String stage, UserPrincipal principal) {
        LambdaQueryWrapper<Warning> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(Warning::getStatus, status);
        if (level != null && !level.isBlank()) q.eq(Warning::getLevel, level);
        if (stage != null && !stage.isBlank()) q.eq(Warning::getStage, stage);
        if (!"R1".equals(principal.getRoleCode())) {
            List<Long> ids = personIds(principal.getBranchId());
            if (ids.isEmpty()) return new PageResult<>(page, size, 0, List.of());
            q.in(Warning::getPersonId, ids);
        } else if (branchId != null) {
            List<Long> ids = personIds(branchId);
            if (ids.isEmpty()) return new PageResult<>(page, size, 0, List.of());
            q.in(Warning::getPersonId, ids);
        }
        q.orderByAsc(Warning::getDueDate).orderByAsc(Warning::getWarningId);
        Page<Warning> p = new Page<>(page, size);
        Page<Warning> result = warningMapper.selectPage(p, q);
        List<WarningVO> items = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    public Map<String, Object> summary(Long branchId, UserPrincipal principal) {
        LambdaQueryWrapper<Warning> q = new LambdaQueryWrapper<Warning>()
                .eq(Warning::getStatus, "OPEN");
        if (!"R1".equals(principal.getRoleCode())) {
            q.in(Warning::getPersonId, personIds(principal.getBranchId()));
        } else if (branchId != null) {
            q.in(Warning::getPersonId, personIds(branchId));
        }
        List<Warning> warnings = warningMapper.selectList(q);
        int info = 0, near = 0, urgent = 0, overdue = 0;
        for (Warning w : warnings) {
            switch (w.getLevel()) {
                case "INFO" -> info++;
                case "NEAR" -> near++;
                case "URGENT" -> urgent++;
                case "OVERDUE" -> overdue++;
                default -> {
                }
            }
        }
        long handled = warningMapper.selectCount(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getStatus, "HANDLED"));
        return Map.of("open", warnings.size(), "info", info, "near", near,
                "urgent", urgent, "overdue", overdue, "handled", handled);
    }

    @Transactional
    public void handle(Long id, WarningHandleRequest request, UserPrincipal principal) {
        Warning warning = warningMapper.selectById(id);
        if (warning == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "预警不存在");
        }
        personService.getEntity(warning.getPersonId(), principal);
        if (!Set.of("HANDLED", "IGNORED").contains(request.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "处置状态只能为 HANDLED 或 IGNORED");
        }
        warning.setStatus(request.getStatus());
        warning.setHandledBy(principal.getUserId());
        warning.setHandledAt(LocalDateTime.now());
        warning.setHandleNote(request.getNote());
        warningMapper.updateById(warning);
    }

    @Transactional
    public int generateAll() {
        List<Person> persons = personMapper.selectList(new LambdaQueryWrapper<Person>()
                .eq(Person::getStatus, "ACTIVE"));
        int count = 0;
        for (Person person : persons) {
            count += generateForPerson(person);
        }
        return count;
    }

    public int generateForPerson(Person person) {
        List<TimeRule> rules = timeRuleMapper.selectList(new LambdaQueryWrapper<TimeRule>()
                .eq(TimeRule::getEnabled, 1));
        int created = 0;
        LocalDate today = LocalDate.now();
        for (TimeRule rule : rules) {
            if (!applyToStage(rule, person.getCurrentStage())) {
                continue;
            }
            if ("EVENT".equals(rule.getNodeType()) || "PREV".equals(rule.getNodeType())) {
                continue;
            }
            LocalDate due;
            String cycleKey;
            if ("QUARTER".equals(rule.getNodeType()) || "T-03".equals(rule.getNodeCode())) {
                due = quarterEnd(today);
                int q = (today.getMonthValue() - 1) / 3 + 1;
                cycleKey = today.getYear() + "Q" + q;
            } else {
                LocalDate start = switch (rule.getStartEvent()) {
                    case "APPLICATION_DATE" -> person.getApplicationDate();
                    case "STAGE_START", "APPROVAL_DATE" -> person.getStageStartDate();
                    default -> null;
                };
                if (start == null) {
                    continue;
                }
                due = addOffset(start, rule);
                cycleKey = "S" + person.getCurrentStage() + "-" + person.getStageStartDate();
            }
            String level = WarningLevelCalculator.level(due, today, rule.getWarnOffsets());
            if ("NONE".equals(level)) {
                continue;
            }
            Warning warning = warningMapper.selectOne(new LambdaQueryWrapper<Warning>()
                    .eq(Warning::getPersonId, person.getPersonId())
                    .eq(Warning::getNodeCode, rule.getNodeCode())
                    .eq(Warning::getCycleKey, cycleKey)
                    .last("limit 1"));
            if (warning == null) {
                warning = new Warning();
                warning.setPersonId(person.getPersonId());
                warning.setStage(person.getCurrentStage());
                warning.setTimeRuleId(rule.getRuleId());
                warning.setNodeCode(rule.getNodeCode());
                warning.setCycleKey(cycleKey);
                warning.setDueDate(due);
                warning.setLevel(level);
                warning.setStatus("OPEN");
                warning.setMessageSent(0);
                warningMapper.insert(warning);
                notifyWarning(warning, rule, person);
                created++;
            } else if ("OPEN".equals(warning.getStatus())) {
                int oldSeverity = WarningLevelCalculator.severity(warning.getLevel());
                int newSeverity = WarningLevelCalculator.severity(level);
                warning.setDueDate(due);
                warning.setLevel(level);
                warningMapper.updateById(warning);
                if (newSeverity > oldSeverity) {
                    notifyWarning(warning, rule, person);
                }
            }
        }
        return created;
    }

    public List<CalendarItemVO> calendar(int year, Long branchId, UserPrincipal principal) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        LambdaQueryWrapper<Warning> q = new LambdaQueryWrapper<Warning>()
                .between(Warning::getDueDate, start, end)
                .orderByAsc(Warning::getDueDate);
        if (!"R1".equals(principal.getRoleCode())) {
            q.in(Warning::getPersonId, personIds(principal.getBranchId()));
        } else if (branchId != null) {
            q.in(Warning::getPersonId, personIds(branchId));
        }
        List<Warning> warnings = warningMapper.selectList(q);
        Map<Long, Person> personMap = new HashMap<>();
        Map<String, String> ruleNames = new HashMap<>();
        for (TimeRule rule : timeRuleMapper.selectList(null)) {
            ruleNames.put(rule.getNodeCode(), rule.getNodeName());
        }
        List<CalendarItemVO> items = new ArrayList<>();
        for (Warning w : warnings) {
            Person person = personMap.computeIfAbsent(w.getPersonId(), personMapper::selectById);
            items.add(new CalendarItemVO(w.getWarningId(), w.getDueDate().toString(),
                    person == null ? "" : person.getName(),
                    ruleNames.getOrDefault(w.getNodeCode(), w.getNodeCode()),
                    w.getLevel(), w.getStatus()));
        }
        return items;
    }

    public ScheduleResponse reverse(ReverseScheduleRequest request, UserPrincipal principal) {
        Person person = personService.getEntity(request.getPersonId(), principal);
        List<TimeRule> rules = timeRuleMapper.selectList(new LambdaQueryWrapper<TimeRule>()
                .eq(TimeRule::getEnabled, 1));
        ScheduleResponse response;
        try {
            response = ScheduleCalculator.calculate(rules, request.getTargetNode(), request.getTargetDate(), LocalDate.now());
        } catch (Exception e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "倒推排期失败: " + e.getMessage());
        }
        response.setPersonId(person.getPersonId());
        response.setPersonName(person.getName());
        return response;
    }

    private void notifyWarning(Warning warning, TimeRule rule, Person person) {
        List<SysUser> receivers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getBranchId, person.getBranchId())
                .in(SysUser::getRoleCode, List.of("R2", "R3"))
                .eq(SysUser::getStatus, 1));
        for (SysUser user : receivers) {
            messageService.send(user.getUserId(), "WARNING", "时限预警：" + rule.getNodeName(),
                    "人员 " + person.getName() + " 的节点 " + rule.getNodeCode()
                            + " 应完成日为 " + warning.getDueDate() + "，当前级别 " + warning.getLevel(),
                    "WARNING", warning.getWarningId());
        }
        warning.setLastNotifiedLevel(warning.getLevel());
        warning.setMessageSent(1);
        warningMapper.updateById(warning);
    }

    private boolean applyToStage(TimeRule rule, String stage) {
        if ("T-03".equals(rule.getNodeCode())) {
            return List.of("S1", "S2", "S3").contains(stage);
        }
        return rule.getStage() == null || rule.getStage().equals(stage);
    }

    private LocalDate addOffset(LocalDate start, TimeRule rule) {
        int value = rule.getOffsetValue() == null ? 0 : rule.getOffsetValue();
        return "MONTH".equalsIgnoreCase(rule.getOffsetUnit()) ? start.plusMonths(value) : start.plusDays(value);
    }

    private LocalDate quarterEnd(LocalDate date) {
        int q = (date.getMonthValue() - 1) / 3 + 1;
        return YearMonth.of(date.getYear(), q * 3).atEndOfMonth();
    }

    private List<Long> personIds(Long branchId) {
        return personMapper.selectList(new LambdaQueryWrapper<Person>().eq(Person::getBranchId, branchId))
                .stream().map(Person::getPersonId).toList();
    }

    private WarningVO toVO(Warning warning) {
        WarningVO vo = new WarningVO();
        org.springframework.beans.BeanUtils.copyProperties(warning, vo);
        Person person = personMapper.selectById(warning.getPersonId());
        if (person != null) {
            vo.setPersonName(person.getName());
            vo.setStudentNo(person.getStudentNo());
            vo.setBranchId(person.getBranchId());
        }
        TimeRule rule = timeRuleMapper.selectOne(new LambdaQueryWrapper<TimeRule>()
                .eq(TimeRule::getNodeCode, warning.getNodeCode()).last("limit 1"));
        vo.setNodeName(rule == null ? warning.getNodeCode() : rule.getNodeName());
        vo.setDaysLeft(ChronoUnit.DAYS.between(LocalDate.now(), warning.getDueDate()));
        if (warning.getHandledBy() != null) {
            SysUser user = userMapper.selectById(warning.getHandledBy());
            vo.setHandledByName(user == null ? null : user.getRealName());
        }
        return vo;
    }
}