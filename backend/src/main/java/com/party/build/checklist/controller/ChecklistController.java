package com.party.build.checklist.controller;

import com.party.build.checklist.domain.ChecklistTemplate;
import com.party.build.checklist.domain.MaterialType;
import com.party.build.checklist.dto.*;
import com.party.build.checklist.service.ChecklistService;
import com.party.build.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChecklistController {
    private final ChecklistService checklistService;

    @GetMapping("/material-types")
    public Result<List<MaterialType>> materialTypes() {
        return Result.ok(checklistService.listMaterialTypes());
    }

    @PostMapping("/material-types")
    @PreAuthorize("hasRole('R1')")
    public Result<MaterialType> createMaterialType(@Valid @RequestBody MaterialTypeRequest request) {
        return Result.ok(checklistService.createMaterialType(request));
    }

    @PutMapping("/material-types/{id}")
    @PreAuthorize("hasRole('R1')")
    public Result<MaterialType> updateMaterialType(@PathVariable Long id,
                                                   @Valid @RequestBody MaterialTypeRequest request) {
        return Result.ok(checklistService.updateMaterialType(id, request));
    }

    @GetMapping("/checklists/templates")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<List<ChecklistTemplate>> templates() {
        return Result.ok(checklistService.listTemplates());
    }

    @GetMapping("/checklists/templates/{id}")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<ChecklistTemplateVO> template(@PathVariable Long id) {
        return Result.ok(checklistService.getTemplate(id));
    }

    @PostMapping("/checklists/templates")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistTemplateVO> createTemplate(@Valid @RequestBody ChecklistTemplateRequest request) {
        return Result.ok(checklistService.createTemplate(request));
    }

    @PutMapping("/checklists/templates/{id}")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistTemplateVO> updateTemplate(@PathVariable Long id,
                                                      @Valid @RequestBody ChecklistTemplateRequest request) {
        return Result.ok(checklistService.updateTemplate(id, request));
    }

    @PostMapping("/checklists/templates/{id}/clone")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistTemplateVO> cloneTemplate(@PathVariable Long id) {
        return Result.ok(checklistService.cloneTemplate(id));
    }

    @PostMapping("/checklists/templates/{id}/validate")
    @PreAuthorize("hasRole('R1')")
    public Result<TemplateValidateResponse> validate(@PathVariable Long id) {
        return Result.ok(checklistService.validate(id));
    }

    @PostMapping("/checklists/templates/{id}/publish")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistTemplateVO> publish(@PathVariable Long id) {
        return Result.ok(checklistService.publish(id));
    }

    @PostMapping("/checklists/templates/{id}/items")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistItemVO> addItem(@PathVariable Long id,
                                           @Valid @RequestBody ChecklistItemRequest request) {
        return Result.ok(checklistService.addItem(id, request));
    }

    @PutMapping("/checklists/items/{itemId}")
    @PreAuthorize("hasRole('R1')")
    public Result<ChecklistItemVO> updateItem(@PathVariable Long itemId,
                                              @Valid @RequestBody ChecklistItemRequest request) {
        return Result.ok(checklistService.updateItem(itemId, request));
    }

    @DeleteMapping("/checklists/items/{itemId}")
    @PreAuthorize("hasRole('R1')")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        checklistService.deleteItem(itemId);
        return Result.ok();
    }
}