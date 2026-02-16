package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "provider_capabilities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_capability", columnNames = {"provider_id", "capability"})
    },
    indexes = {
        @Index(name = "idx_pc_capability_status_priority", columnList = "capability, status, priority")
    }
)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProviderCapability extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ModelProvider provider;

    @Column(nullable = false, length = 64)
    private String capability;

    @Column(length = 512)
    private String endpoint;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(name = "default_timeout_ms", nullable = false)
    private Integer defaultTimeoutMs = 8000;

    @Column(name = "default_params_json", columnDefinition = "JSON")
    private String defaultParamsJson;
}
