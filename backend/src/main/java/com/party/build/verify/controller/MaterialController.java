package com.party.build.verify.controller;

import com.party.build.audit.service.AuditService;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.Result;
import com.party.build.verify.domain.VerifyTask;
import com.party.build.verify.dto.*;
import com.party.build.verify.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;
    private final AuditService auditService;

    @GetMapping("/persons/{personId}/materials")
    public Result<List<MaterialVO>> list(@PathVariable Long personId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(materialService.listByPerson(personId, principal));
    }

    @GetMapping("/materials/{id}")
    public Result<MaterialDetailVO> detail(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        MaterialDetailVO detail = materialService.get(id, principal);
        auditService.log(principal, "VIEW_MATERIAL", "MATERIAL", id, "SUCCESS", "查看材料详情", null);
        return Result.ok(detail);
    }

    @PostMapping("/materials/{id}/versions")
    public Result<UploadResult> upload(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        UploadResult result = materialService.upload(id, file, principal);
        auditService.log(principal, "UPLOAD_MATERIAL", "MATERIAL", id, "SUCCESS",
                "上传版本 v" + result.getVersionNo(), null);
        return Result.ok(result);
    }

    @GetMapping("/materials/{id}/versions")
    public Result<List<MaterialVersionVO>> versions(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        materialService.requireMaterial(id, principal);
        return Result.ok(materialService.versions(id));
    }

    @GetMapping("/materials/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @PathVariable Long versionId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Path path = materialService.resolveVersionFile(id, versionId, principal);
        auditService.log(principal, "DOWNLOAD_MATERIAL", "MATERIAL", id, "SUCCESS",
                "下载材料版本 " + versionId, null);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(path.getFileName().toString(), StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/materials/{id}/verify")
    public Result<VerifyTask> verify(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(materialService.createVerifyTask(id, principal));
    }

    @GetMapping("/verify-tasks/{taskId}")
    public Result<VerifyTask> task(@PathVariable Long taskId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(materialService.getTask(taskId, principal));
    }

    @GetMapping("/materials/{id}/reports/latest")
    public Result<VerifyReportVO> latestReport(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(materialService.latestReport(id, principal));
    }

    @GetMapping("/reports/{reportId}")
    public Result<VerifyReportVO> report(@PathVariable Long reportId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(materialService.getReport(reportId, principal));
    }
}