package com.party.build.review.service;

import com.party.build.audit.service.AuditService;
import com.party.build.audit.service.MessageService;
import com.party.build.auth.domain.SysUser;
import com.party.build.auth.mapper.SysUserMapper;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.checklist.mapper.MaterialTypeMapper;
import com.party.build.common.exception.BizException;
import com.party.build.person.domain.Person;
import com.party.build.person.mapper.PersonMapper;
import com.party.build.person.service.PersonService;
import com.party.build.review.domain.ReviewLog;
import com.party.build.review.dto.ReviewRequest;
import com.party.build.review.mapper.ReviewLogMapper;
import com.party.build.verify.domain.Material;
import com.party.build.verify.domain.MaterialVersion;
import com.party.build.verify.domain.VerifyReport;
import com.party.build.verify.mapper.MaterialMapper;
import com.party.build.verify.mapper.MaterialVersionMapper;
import com.party.build.verify.mapper.VerifyReportMapper;
import com.party.build.verify.mapper.VerifyTaskMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    private ReviewService service(Material material, UserPrincipal principal) {
        ReviewLogMapper logMapper = mock(ReviewLogMapper.class);
        MaterialMapper materialMapper = mock(MaterialMapper.class);
        MaterialVersionMapper versionMapper = mock(MaterialVersionMapper.class);
        VerifyReportMapper reportMapper = mock(VerifyReportMapper.class);
        VerifyTaskMapper taskMapper = mock(VerifyTaskMapper.class);
        MaterialTypeMapper typeMapper = mock(MaterialTypeMapper.class);
        PersonMapper personMapper = mock(PersonMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PersonService personService = mock(PersonService.class);
        MessageService messageService = mock(MessageService.class);
        AuditService auditService = mock(AuditService.class);

        when(materialMapper.selectById(material.getMaterialId())).thenReturn(material);
        Person person = new Person();
        person.setPersonId(material.getPersonId());
        person.setName("张明");
        when(personService.getEntity(material.getPersonId(), principal)).thenReturn(person);
        MaterialVersion version = new MaterialVersion();
        version.setVersionId(1L);
        version.setMaterialId(material.getMaterialId());
        version.setVersionNo(1);
        version.setUploadedBy(4L);
        when(versionMapper.selectById(1L)).thenReturn(version);
        VerifyReport report = new VerifyReport();
        report.setReportId(1L);
        report.setMaterialId(material.getMaterialId());
        report.setVersionId(1L);
        when(reportMapper.selectById(1L)).thenReturn(report);
        return new ReviewService(logMapper, materialMapper, versionMapper, reportMapper, taskMapper,
                typeMapper, personMapper, userMapper, personService, messageService, auditService);
    }

    private Material material() {
        Material m = new Material();
        m.setMaterialId(1L);
        m.setPersonId(1L);
        m.setStatus("M3");
        m.setCurrentVersionId(1L);
        return m;
    }

    private UserPrincipal principal(String role) {
        return new UserPrincipal(1L, "u", "用户", role, 1L, null, 0, true);
    }

    @Test
    void finalPassShouldMoveMaterialToM4() {
        Material m = material();
        ReviewService service = service(m, principal("R2"));
        ReviewRequest req = new ReviewRequest();
        req.setConclusion("PASS");
        req.setVersionId(1L);
        req.setReportId(1L);
        ReviewLog log = service.review(1L, req, principal("R2"));
        assertThat(log.getReviewLevel()).isEqualTo("FINAL");
        assertThat(m.getStatus()).isEqualTo("M4");
    }

    @Test
    void initialPassShouldKeepMaterialAtM3() {
        Material m = material();
        ReviewService service = service(m, principal("R3"));
        ReviewRequest req = new ReviewRequest();
        req.setConclusion("PASS");
        req.setVersionId(1L);
        req.setReportId(1L);
        ReviewLog log = service.review(1L, req, principal("R3"));
        assertThat(log.getReviewLevel()).isEqualTo("INITIAL");
        assertThat(m.getStatus()).isEqualTo("M3");
    }

    @Test
    void returnRequiresOpinion() {
        Material m = material();
        ReviewService service = service(m, principal("R2"));
        ReviewRequest req = new ReviewRequest();
        req.setConclusion("RETURN");
        req.setReasonCode("MATERIAL_ISSUE");
        req.setOpinion("太短");
        req.setVersionId(1L);
        req.setReportId(1L);
        assertThatThrownBy(() -> service.review(1L, req, principal("R2")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("10");
    }
}