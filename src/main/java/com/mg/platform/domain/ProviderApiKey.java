package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "provider_api_keys")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProviderApiKey extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ModelProvider provider;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "api_key_cipher", nullable = false, columnDefinition = "TEXT")
    private String apiKeyCipher;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";
}
