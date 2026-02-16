package com.mg.platform.common.dto;

import com.mg.platform.domain.ProviderApiKey;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ProviderApiKey DTO，用于 API 响应，不回显明文 API Key
 */
@Data
public class ProviderApiKeyDTO {
    private Long id;
    private Long providerId;
    private String providerCode;
    private String name;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 不包含 apiKeyCipher 字段，确保不回显明文

    public static ProviderApiKeyDTO from(ProviderApiKey apiKey) {
        ProviderApiKeyDTO dto = new ProviderApiKeyDTO();
        dto.setId(apiKey.getId());
        dto.setProviderId(apiKey.getProvider().getId());
        dto.setProviderCode(apiKey.getProvider().getCode());
        dto.setName(apiKey.getName());
        dto.setStatus(apiKey.getStatus());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setUpdatedAt(apiKey.getUpdatedAt());
        return dto;
    }
}
