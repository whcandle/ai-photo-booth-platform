package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "capability_routing_policies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_scope_capability", columnNames = {"scope", "merchant_id", "capability"})
    },
    indexes = {
        @Index(name = "idx_policy_capability_status", columnList = "capability, status")
    }
)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CapabilityRoutingPolicy extends BaseEntity {
    @Column(nullable = false, length = 16)
    private String scope = "GLOBAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false, length = 64)
    private String capability;

    @Column(name = "prefer_providers_json", columnDefinition = "JSON")
    private String preferProvidersJson;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "failover_on_http_codes_json", columnDefinition = "JSON")
    private String failoverOnHttpCodesJson;

    @Column(name = "max_cost_tier")
    private Integer maxCostTier;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
