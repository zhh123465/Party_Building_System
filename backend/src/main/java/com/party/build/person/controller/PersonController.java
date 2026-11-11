package com.party.build.person.controller;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import com.party.build.person.dto.PersonCreateRequest;
import com.party.build.person.dto.PersonVO;
import com.party.build.person.dto.ImportResult;
import com.party.build.person.dto.PersonImportRow;
import com.party.build.person.service.ImportService;
import com.party.build.person.service.PersonService;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;
    private final ImportService importService;

    @GetMapping
    public Result<PageResult<PersonVO>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) Long branchId,
                                             @RequestParam(required = false) String stage,
                                             @RequestParam(required = false) String keyword,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(personService.list(page, Math.min(size, 100), branchId, stage, keyword, principal));
    }

    @GetMapping("/{id}")
    public Result<PersonVO> get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(personService.get(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<PersonVO> create(@Valid @RequestBody PersonCreateRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(personService.create(request, principal));
    }

    @GetMapping("/import/template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("人员导入模板", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), PersonImportRow.class)
                .sheet("人员导入").doWrite(List.of());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<ImportResult> importExcel(@RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return Result.ok(importService.importExcel(file.getInputStream(), principal));
    }

    @GetMapping("/{id}/id-card")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<String> idCard(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(personService.decryptIdCard(id, principal));
    }
}