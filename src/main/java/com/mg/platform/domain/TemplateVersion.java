package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "template_versions")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TemplateVersion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(name = "package_url", nullable = false, length = 512)
    private String packageUrl;

    @Column(length = 128)
    private String checksum;

    @Column(name = "manifest_json", columnDefinition = "JSON")
    private String manifestJson;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE"; // ACTIVE/ROLLBACK
}
