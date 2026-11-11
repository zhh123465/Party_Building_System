package com.party.build.person.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.PageResult;
import com.party.build.common.exception.BizException;
import com.party.build.common.util.CryptoService;
import com.party.build.person.domain.Person;
import com.party.build.person.dto.PersonCreateRequest;
import com.party.build.person.dto.PersonVO;
import com.party.build.person.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonMapper personMapper;
    private final CryptoService cryptoService;

    public PageResult<PersonVO> list(long page, long size, Long branchId, String stage, String keyword,
                                     UserPrincipal principal) {
        Page<Person> p = new Page<>(page, size);
        LambdaQueryWrapper<Person> q = new LambdaQueryWrapper<>();
        applyScope(q, principal);
        if ("R1".equals(principal.getRoleCode()) && branchId != null) {
            q.eq(Person::getBranchId, branchId);
        }
        if (stage != null && !stage.isBlank()) {
            q.eq(Person::getCurrentStage, stage);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(Person::getName, keyword).or().like(Person::getStudentNo, keyword));
        }
        q.orderByDesc(Person::getPersonId);
        Page<Person> result = personMapper.selectPage(p, q);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::toVO).toList());
    }

    public PersonVO get(Long personId, UserPrincipal principal) {
        Person person = personMapper.selectById(personId);
        if (person == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "人员不存在");
        }
        checkScope(person, principal);
        return toVO(person);
    }

    public PersonVO create(PersonCreateRequest request, UserPrincipal principal) {
        if (!"R1".equals(principal.getRoleCode())) {
            if (!principal.getBranchId().equals(request.getBranchId())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }
        Long exists = personMapper.selectCount(new LambdaQueryWrapper<Person>()
                .eq(Person::getStudentNo, request.getStudentNo()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.ALREADY_EXISTS, "学号已存在");
        }
        Person p = new Person();
        p.setStudentNo(request.getStudentNo());
        p.setName(request.getName());
        p.setGender(request.getGender());
        p.setCollege(request.getCollege());
        p.setMajor(request.getMajor());
        p.setClassName(request.getClassName());
        p.setBranchId(request.getBranchId());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            p.setPhoneCipher(cryptoService.encrypt(request.getPhone()));
            p.setPhoneMask(cryptoService.maskPhone(request.getPhone()));
        }
        if (request.getIdCard() != null && !request.getIdCard().isBlank()) {
            p.setIdCardCipher(cryptoService.encrypt(request.getIdCard()));
            p.setIdCardMask(cryptoService.maskIdCard(request.getIdCard()));
        }
        p.setApplicationDate(request.getApplicationDate());
        p.setCurrentStage(request.getCurrentStage() == null ? "S0" : request.getCurrentStage());
        p.setStageStartDate(request.getStageStartDate());
        p.setTemplateId(1L);
        p.setStatus("ACTIVE");
        p.setRemark(request.getRemark());
        personMapper.insert(p);
        return toVO(p);
    }

    public Person getEntity(Long personId, UserPrincipal principal) {
        Person person = personMapper.selectById(personId);
        if (person == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "人员不存在");
        }
        checkScope(person, principal);
        return person;
    }

    public String decryptIdCard(Long personId, UserPrincipal principal) {
        Person person = personMapper.selectById(personId);
        if (person == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "人员不存在");
        }
        checkScope(person, principal);
        if (!"R1".equals(principal.getRoleCode()) && !"R2".equals(principal.getRoleCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅支部书记及以上角色可查看完整身份证号");
        }
        return cryptoService.decrypt(person.getIdCardCipher());
    }

    private void applyScope(LambdaQueryWrapper<Person> q, UserPrincipal principal) {
        switch (principal.getRoleCode()) {
            case "R1" -> {
            }
            case "R2", "R3" -> q.eq(Person::getBranchId, principal.getBranchId());
            case "R4" -> {
                if (principal.getPersonId() == null) {
                    throw new BizException(ErrorCode.FORBIDDEN);
                }
                q.eq(Person::getPersonId, principal.getPersonId());
            }
            default -> throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    public void checkScope(Person person, UserPrincipal principal) {
        switch (principal.getRoleCode()) {
            case "R1" -> {
            }
            case "R2", "R3" -> {
                if (!person.getBranchId().equals(principal.getBranchId())) {
                    throw new BizException(ErrorCode.FORBIDDEN);
                }
            }
            case "R4" -> {
                if (principal.getPersonId() == null || !person.getPersonId().equals(principal.getPersonId())) {
                    throw new BizException(ErrorCode.FORBIDDEN);
                }
            }
            default -> throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private PersonVO toVO(Person p) {
        PersonVO vo = new PersonVO();
        vo.setPersonId(p.getPersonId());
        vo.setStudentNo(p.getStudentNo());
        vo.setName(p.getName());
        vo.setGender(p.getGender());
        vo.setCollege(p.getCollege());
        vo.setMajor(p.getMajor());
        vo.setClassName(p.getClassName());
        vo.setBranchId(p.getBranchId());
        vo.setPhoneMask(p.getPhoneMask());
        vo.setIdCardMask(p.getIdCardMask());
        vo.setApplicationDate(p.getApplicationDate());
        vo.setCurrentStage(p.getCurrentStage());
        vo.setStageStartDate(p.getStageStartDate());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        return vo;
    }
}