package com.mg.platform.web.admin;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.domain.Template;
import com.mg.platform.domain.TemplateVersion;
import com.mg.platform.service.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/templates")
    public ApiResponse<List<Template>> getAllTemplates() {
        try {
            List<Template> templates = adminService.getAllTemplates();
            return ApiResponse.success(templates);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/templates")
    public ApiResponse<Template> createTemplate(@RequestBody AdminService.CreateTemplateRequest request) {
        try {
            Template template = adminService.createTemplate(request);
            return ApiResponse.success(template);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/templates/{templateId}/versions")
    public ApiResponse<TemplateVersion> createVersion(
            @PathVariable Long templateId,
            @RequestBody AdminService.CreateVersionRequest request
    ) {
        try {
            TemplateVersion version = adminService.createTemplateVersion(templateId, request);
            return ApiResponse.success(version);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
