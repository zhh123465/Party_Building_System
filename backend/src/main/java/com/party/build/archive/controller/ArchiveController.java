package com.party.build.archive.controller;

import com.party.build.archive.dto.ArchiveRecordVO;
import com.party.build.archive.dto.PersonArchiveVO;
import com.party.build.archive.dto.SearchResultVO;
import com.party.build.archive.service.ArchiveService;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ArchiveController {
    private final ArchiveService archiveService;

    @GetMapping("/persons/{id}/archive/tree")
    public Result<PersonArchiveVO> tree(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(archiveService.tree(id, principal));
    }

    @PostMapping("/persons/{id}/archive")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<ArchiveRecordVO> archive(@PathVariable Long id,
                                           @RequestParam String stage,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(archiveService.archive(id, stage, principal));
    }

    @GetMapping("/persons/{id}/archives")
    public Result<List<ArchiveRecordVO>> archives(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(archiveService.archives(id, principal));
    }

    @GetMapping("/archives/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Path path = archiveService.download(id, principal);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(path.getFileName().toString(), StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/materials/search")
    public Result<PageResult<SearchResultVO>> search(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(archiveService.search(keyword, page, Math.min(size, 100), principal));
    }
}