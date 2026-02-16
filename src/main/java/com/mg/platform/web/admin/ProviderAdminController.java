package com.mg.platform.web.admin;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.common.dto.ProviderApiKeyDTO;
import com.mg.platform.domain.ModelProvider;
import com.mg.platform.domain.ProviderApiKey;
import com.mg.platform.domain.ProviderCapability;
import com.mg.platform.service.ProviderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/providers")
@RequiredArgsConstructor
public class ProviderAdminController {
    private final ProviderAdminService providerAdminService;

    // ========== Providers ==========

    @GetMapping
    public ApiResponse<List<ModelProvider>> listProviders() {
        try {
            List<ModelProvider> providers = providerAdminService.listProviders();
            return ApiResponse.success(providers);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<ModelProvider> createProvider(@RequestBody ProviderAdminService.CreateProviderRequest request) {
        try {
            ModelProvider provider = providerAdminService.createProvider(request);
            return ApiResponse.success(provider);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{providerId}")
    public ApiResponse<ModelProvider> updateProvider(
            @PathVariable Long providerId,
            @RequestBody ProviderAdminService.UpdateProviderRequest request
    ) {
        try {
            ModelProvider provider = providerAdminService.updateProvider(providerId, request);
            return ApiResponse.success(provider);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ========== Capabilities ==========

    @GetMapping("/{providerId}/capabilities")
    public ApiResponse<List<ProviderCapability>> listCapabilities(@PathVariable Long providerId) {
        try {
            List<ProviderCapability> capabilities = providerAdminService.listCapabilities(providerId);
            return ApiResponse.success(capabilities);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{providerId}/capabilities")
    public ApiResponse<ProviderCapability> createCapability(
            @PathVariable Long providerId,
            @RequestBody ProviderAdminService.CreateCapabilityRequest request
    ) {
        try {
            ProviderCapability capability = providerAdminService.createCapability(providerId, request);
            return ApiResponse.success(capability);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{providerId}/capabilities/{capabilityId}")
    public ApiResponse<ProviderCapability> updateCapability(
            @PathVariable Long providerId,
            @PathVariable Long capabilityId,
            @RequestBody ProviderAdminService.UpdateCapabilityRequest request
    ) {
        try {
            ProviderCapability capability = providerAdminService.updateCapability(providerId, capabilityId, request);
            return ApiResponse.success(capability);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ========== API Keys ==========

    @GetMapping("/{providerId}/keys")
    public ApiResponse<List<ProviderApiKeyDTO>> listApiKeys(@PathVariable Long providerId) {
        try {
            List<ProviderApiKey> keys = providerAdminService.listApiKeys(providerId);
            List<ProviderApiKeyDTO> dtos = keys.stream()
                    .map(ProviderApiKeyDTO::from)
                    .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{providerId}/keys")
    public ApiResponse<ProviderApiKeyDTO> createApiKey(
            @PathVariable Long providerId,
            @RequestBody ProviderAdminService.CreateApiKeyRequest request
    ) {
        try {
            ProviderApiKey apiKey = providerAdminService.createApiKey(providerId, request);
            // 不回显明文，使用 DTO 隐藏 apiKeyCipher
            return ApiResponse.success(ProviderApiKeyDTO.from(apiKey));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{providerId}/keys/{keyId}/disable")
    public ApiResponse<ProviderApiKeyDTO> disableApiKey(
            @PathVariable Long providerId,
            @PathVariable Long keyId
    ) {
        try {
            ProviderApiKey apiKey = providerAdminService.disableApiKey(providerId, keyId);
            // 不回显明文，使用 DTO 隐藏 apiKeyCipher
            return ApiResponse.success(ProviderApiKeyDTO.from(apiKey));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
