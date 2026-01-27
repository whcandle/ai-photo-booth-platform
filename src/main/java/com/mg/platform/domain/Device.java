package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Device extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "device_code", nullable = false, length = 64)
    private String deviceCode;

    @Column(length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "client_version", length = 32)
    private String clientVersion;

    @Column(name = "meta_json", columnDefinition = "JSON")
    private String metaJson;
}
