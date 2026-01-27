package com.mg.platform.service;

import com.mg.platform.domain.Template;
import com.mg.platform.domain.TemplateVersion;
import com.mg.platform.repo.TemplateRepository;
import com.mg.platform.repo.TemplateVersionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    public Template createTemplate(CreateTemplateRequest request) {
        Template template = new Template();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setType(request.getType());
        template.setStatus("ACTIVE");
        template.setContentJson(request.getContentJson());
        template.setCoverUrl(request.getCoverUrl());
        return templateRepository.save(template);
    }

    public TemplateVersion createTemplateVersion(Long templateId, CreateVersionRequest request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        TemplateVersion version = new TemplateVersion();
        version.setTemplate(template);
        version.setVersion(request.getVersion());
        version.setPackageUrl(request.getPackageUrl());
        version.setChecksum(request.getChecksum());
        version.setManifestJson(request.getManifestJson());
        version.setStatus("ACTIVE");
        return templateVersionRepository.save(version);
    }

    @Data
    public static class CreateTemplateRequest {
        private String code;
        private String name;
        private String description;
        private String type;
        private String contentJson;
        private String coverUrl;
    }

    @Data
    public static class CreateVersionRequest {
        private String version;
        private String packageUrl;
        private String checksum;
        private String manifestJson;
    }
}
