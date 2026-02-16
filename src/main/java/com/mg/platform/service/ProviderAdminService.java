package com.mg.platform.service;

import com.mg.platform.common.util.CryptoUtil;
import com.mg.platform.domain.ModelProvider;
import com.mg.platform.domain.ProviderApiKey;
import com.mg.platform.domain.ProviderCapability;
import com.mg.platform.repo.ModelProviderRepository;
import com.mg.platform.repo.ProviderApiKeyRepository;
import com.mg.platform.repo.ProviderCapabilityRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderAdminService {
    private final ModelProviderRepository providerRepository;
    private final ProviderCapabilityRepository capabilityRepository;
    private final ProviderApiKeyRepository apiKeyRepository;
    private final CryptoUtil cryptoUtil;

    // ========== Providers ==========

    public List<ModelProvider> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional
    public ModelProvider createProvider(CreateProviderRequest request) {
        // 检查 code 是否已存在
        if (providerRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Provider code already exists: " + request.getCode());
        }

        ModelProvider provider = new ModelProvider();
        provider.setCode(request.getCode());
        provider.setName(request.getName());
        provider.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return providerRepository.save(provider);
    }

    @Transactional
    public ModelProvider updateProvider(Long providerId, UpdateProviderRequest request) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        if (request.getName() != null) {
            provider.setName(request.getName());
        }
        if (request.getStatus() != null) {
            provider.setStatus(request.getStatus());
        }
        // code 不允许修改
        return providerRepository.save(provider);
    }

    // ========== Capabilities ==========

    public List<ProviderCapability> listCapabilities(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));
        return capabilityRepository.findByProviderId(providerId);
    }

    @Transactional
    public ProviderCapability createCapability(Long providerId, CreateCapabilityRequest request) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        // 检查是否已存在相同的 capability
        if (capabilityRepository.findByProviderIdAndCapability(providerId, request.getCapability()).isPresent()) {
            throw new RuntimeException("Capability already exists for this provider: " + request.getCapability());
        }

        ProviderCapability capability = new ProviderCapability();
        capability.setProvider(provider);
        capability.setCapability(request.getCapability());
        capability.setEndpoint(request.getEndpoint());
        capability.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        capability.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        capability.setDefaultTimeoutMs(request.getDefaultTimeoutMs() != null ? request.getDefaultTimeoutMs() : 8000);
        capability.setDefaultParamsJson(request.getDefaultParamsJson());
        return capabilityRepository.save(capability);
    }

    @Transactional
    public ProviderCapability updateCapability(Long providerId, Long capabilityId, UpdateCapabilityRequest request) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        ProviderCapability capability = capabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new RuntimeException("Capability not found: " + capabilityId));

        // 验证 capability 属于该 provider
        if (!capability.getProvider().getId().equals(providerId)) {
            throw new RuntimeException("Capability does not belong to provider: " + providerId);
        }

        if (request.getEndpoint() != null) {
            capability.setEndpoint(request.getEndpoint());
        }
        if (request.getStatus() != null) {
            capability.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            capability.setPriority(request.getPriority());
        }
        if (request.getDefaultTimeoutMs() != null) {
            capability.setDefaultTimeoutMs(request.getDefaultTimeoutMs());
        }
        if (request.getDefaultParamsJson() != null) {
            capability.setDefaultParamsJson(request.getDefaultParamsJson());
        }
        // capability 字段不允许修改

        return capabilityRepository.save(capability);
    }

    // ========== API Keys ==========

    public List<ProviderApiKey> listApiKeys(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));
        return apiKeyRepository.findByProviderIdAndStatus(providerId, "ACTIVE");
    }

    @Transactional
    public ProviderApiKey createApiKey(Long providerId, CreateApiKeyRequest request) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        // 加密 API Key
        String encryptedKey = cryptoUtil.encrypt(request.getApiKey());

        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setProvider(provider);
        apiKey.setName(request.getName());
        apiKey.setApiKeyCipher(encryptedKey);
        apiKey.setStatus("ACTIVE");
        return apiKeyRepository.save(apiKey);
    }

    @Transactional
    public ProviderApiKey disableApiKey(Long providerId, Long keyId) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        ProviderApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API Key not found: " + keyId));

        // 验证 key 属于该 provider
        if (!apiKey.getProvider().getId().equals(providerId)) {
            throw new RuntimeException("API Key does not belong to provider: " + providerId);
        }

        apiKey.setStatus("INACTIVE");
        return apiKeyRepository.save(apiKey);
    }

    // ========== DTOs ==========

    @Data
    public static class CreateProviderRequest {
        private String code;
        private String name;
        private String status;
    }

    @Data
    public static class UpdateProviderRequest {
        private String name;
        private String status;
    }

    @Data
    public static class CreateCapabilityRequest {
        private String capability;
        private String endpoint;
        private String status;
        private Integer priority;
        private Integer defaultTimeoutMs;
        private String defaultParamsJson;
    }

    @Data
    public static class UpdateCapabilityRequest {
        private String endpoint;
        private String status;
        private Integer priority;
        private Integer defaultTimeoutMs;
        private String defaultParamsJson;
    }

    @Data
    public static class CreateApiKeyRequest {
        private String name;
        private String apiKey; // 明文，后端加密
    }
}
