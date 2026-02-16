package com.mg.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.platform.common.dto.AiResolveRequest;
import com.mg.platform.common.dto.AiResolveResponse;
import com.mg.platform.common.exception.NoActiveApiKeyException;
import com.mg.platform.common.util.CryptoUtil;
import com.mg.platform.domain.CapabilityRoutingPolicy;
import com.mg.platform.domain.ModelProvider;
import com.mg.platform.domain.ProviderApiKey;
import com.mg.platform.domain.ProviderCapability;
import com.mg.platform.domain.Merchant;
import com.mg.platform.repo.CapabilityRoutingPolicyRepository;
import com.mg.platform.repo.MerchantRepository;
import com.mg.platform.repo.ProviderApiKeyRepository;
import com.mg.platform.repo.ProviderCapabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolveService {
    private final ProviderCapabilityRepository capabilityRepository;
    private final CapabilityRoutingPolicyRepository routingPolicyRepository;
    private final ProviderApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final CryptoUtil cryptoUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public AiResolveResponse resolve(AiResolveRequest request) {
        // 1. 根据 request.capability 查询 provider_capabilities(status='ACTIVE') 按 priority ASC
        List<ProviderCapability> capabilities = capabilityRepository
                .findByCapabilityAndStatusOrderByPriorityAsc(request.getCapability(), "ACTIVE");

        if (capabilities.isEmpty()) {
            throw new RuntimeException("No active provider found for capability: " + request.getCapability());
        }

        // 2. 根据优先级获取 prefer：请求 prefer > merchant policy > global policy
        List<String> finalPrefer = getPreferWithPriority(
                request.getPrefer(),
                request.getMerchantCode(),
                request.getCapability()
        );

        // 3. 按 prefer 顺序筛选和排序 capabilities
        List<ProviderCapability> sortedCapabilities = sortByPrefer(capabilities, finalPrefer);

        // 4. 如果 constraints.maxCostTier 有值，过滤超出成本的 provider
        if (request.getConstraints() != null && request.getConstraints().getMaxCostTier() != null) {
            sortedCapabilities = filterByCostTier(sortedCapabilities, request.getConstraints().getMaxCostTier());
            if (sortedCapabilities.isEmpty()) {
                throw new RuntimeException("No provider found within cost tier: " + request.getConstraints().getMaxCostTier());
            }
        }

        // 5. 选择第一个可用 provider
        ProviderCapability selectedCapability = sortedCapabilities.get(0);
        ModelProvider provider = selectedCapability.getProvider();

        // 6. providerCode 来自 model_providers.code
        String providerCode = provider.getCode();

        // 7. endpoint 来自 provider_capabilities.endpoint
        String endpoint = selectedCapability.getEndpoint();

        // 8. timeoutMs = request.constraints.timeoutMs ?? provider_capabilities.default_timeout_ms
        Integer timeoutMs = request.getConstraints() != null && request.getConstraints().getTimeoutMs() != null
                ? request.getConstraints().getTimeoutMs()
                : selectedCapability.getDefaultTimeoutMs();

        // 9. params = merge(provider_capabilities.default_params_json, request.hintParams)（hint 覆盖同名键）
        Map<String, Object> params = mergeParams(selectedCapability.getDefaultParamsJson(), request.getHintParams());

        // 10. 查询并解密 API Key
        AiResolveResponse.Auth auth = getApiKeyAuth(provider.getId());

        // 11. 构建响应
        AiResolveResponse response = new AiResolveResponse();
        response.setMode("direct");
        response.setCapability(request.getCapability());

        AiResolveResponse.Direct direct = new AiResolveResponse.Direct();
        direct.setProviderCode(providerCode);
        direct.setEndpoint(endpoint);
        direct.setAuth(auth);
        direct.setTimeoutMs(timeoutMs);
        direct.setParams(params);

        response.setDirect(direct);

        return response;
    }

    /**
     * 根据优先级获取 prefer：请求 prefer > merchant policy > global policy
     * @param requestPrefer 请求的 prefer
     * @param merchantCode 商家代码
     * @param capability 能力名称
     * @return 最终的 prefer 列表
     */
    private List<String> getPreferWithPriority(List<String> requestPrefer, String merchantCode, String capability) {
        // 优先级 1: 请求 prefer（最高优先级）
        if (requestPrefer != null && !requestPrefer.isEmpty()) {
            return requestPrefer;
        }

        // 优先级 2: Merchant policy
        if (merchantCode != null && !merchantCode.trim().isEmpty()) {
            List<String> merchantPrefer = getMerchantPreferFromPolicy(merchantCode, capability);
            if (!merchantPrefer.isEmpty()) {
                return merchantPrefer;
            }
        }

        // 优先级 3: Global policy
        List<String> globalPrefer = getGlobalPreferFromPolicy(capability);
        if (!globalPrefer.isEmpty()) {
            return globalPrefer;
        }

        // 无 prefer，返回空列表（将按 priority 排序）
        return Collections.emptyList();
    }

    /**
     * 从 Merchant routing policy 获取 prefer
     */
    private List<String> getMerchantPreferFromPolicy(String merchantCode, String capability) {
        // 根据 merchantCode 查询 merchant_id
        Optional<Merchant> merchantOpt = merchantRepository.findByCode(merchantCode);
        if (merchantOpt.isEmpty()) {
            log.debug("Merchant not found for code: {}", merchantCode);
            return Collections.emptyList();
        }

        Long merchantId = merchantOpt.get().getId();

        // 查询 MERCHANT scope 的 policy（使用 findFirst 避免重复记录问题）
        Optional<CapabilityRoutingPolicy> policyOpt = routingPolicyRepository
                .findFirstByScopeAndMerchantIdAndCapabilityOrderByCreatedAtDesc("MERCHANT", merchantId, capability);

        if (policyOpt.isEmpty()) {
            return Collections.emptyList();
        }

        CapabilityRoutingPolicy policy = policyOpt.get();
        if (!"ACTIVE".equals(policy.getStatus())) {
            return Collections.emptyList();
        }

        String preferProvidersJson = policy.getPreferProvidersJson();
        if (preferProvidersJson == null || preferProvidersJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(preferProvidersJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse preferProvidersJson for merchant policy: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 从 GLOBAL routing policy 获取 prefer
     */
    private List<String> getGlobalPreferFromPolicy(String capability) {
        // 使用 findFirst 避免重复记录问题（按创建时间降序，获取最新的）
        Optional<CapabilityRoutingPolicy> policyOpt = routingPolicyRepository
                .findFirstByScopeAndMerchantIdAndCapabilityOrderByCreatedAtDesc("GLOBAL", null, capability);

        if (policyOpt.isEmpty()) {
            return Collections.emptyList();
        }

        CapabilityRoutingPolicy policy = policyOpt.get();
        if (!"ACTIVE".equals(policy.getStatus())) {
            return Collections.emptyList();
        }

        String preferProvidersJson = policy.getPreferProvidersJson();
        if (preferProvidersJson == null || preferProvidersJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(preferProvidersJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse preferProvidersJson for global policy: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据 cost_tier 过滤 provider（如果字段存在）
     * 如果 cost_tier 字段不存在，则忽略过滤
     */
    private List<ProviderCapability> filterByCostTier(List<ProviderCapability> capabilities, Integer maxCostTier) {
        if (maxCostTier == null) {
            return capabilities;
        }

        // 检查 ModelProvider 是否有 cost_tier 字段
        try {
            Field costTierField = ModelProvider.class.getDeclaredField("costTier");
            // 字段存在，进行过滤
            return capabilities.stream()
                    .filter(cap -> {
                        ModelProvider provider = cap.getProvider();
                        try {
                            costTierField.setAccessible(true);
                            Integer costTier = (Integer) costTierField.get(provider);
                            return costTier == null || costTier <= maxCostTier;
                        } catch (Exception e) {
                            // 获取字段值失败，保留该 provider
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (NoSuchFieldException e) {
            // cost_tier 字段不存在，忽略过滤
            log.debug("cost_tier field not found in ModelProvider, ignoring maxCostTier filter");
            return capabilities;
        }
    }

    /**
     * 根据 prefer 列表对 capabilities 进行排序
     * prefer 中的 provider code 优先，然后按原有 priority 顺序
     */
    private List<ProviderCapability> sortByPrefer(List<ProviderCapability> capabilities, List<String> prefer) {
        if (prefer == null || prefer.isEmpty()) {
            return capabilities;
        }

        // 获取所有 provider codes 的映射
        Map<String, ProviderCapability> capabilityMap = capabilities.stream()
                .collect(Collectors.toMap(
                        c -> c.getProvider().getCode(),
                        c -> c,
                        (c1, c2) -> c1 // 如果有重复，保留第一个
                ));

        List<ProviderCapability> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        List<String> missingProviders = new ArrayList<>();

        // 先添加 prefer 中的（按 prefer 顺序）
        for (String preferCode : prefer) {
            ProviderCapability cap = capabilityMap.get(preferCode);
            if (cap != null && !added.contains(preferCode)) {
                result.add(cap);
                added.add(preferCode);
            } else if (cap == null) {
                missingProviders.add(preferCode);
            }
        }

        // 如果 prefer 中有不存在的 provider，记录警告
        if (!missingProviders.isEmpty()) {
            log.warn("Some preferred providers not found in capabilities: {}", missingProviders);
        }

        // 如果 prefer 中的 provider 都不存在，返回原始列表（按 priority）
        if (result.isEmpty()) {
            log.warn("None of the preferred providers found, falling back to priority order");
            return capabilities;
        }

        // 再添加剩余的（保持原有 priority 顺序）
        for (ProviderCapability cap : capabilities) {
            String code = cap.getProvider().getCode();
            if (!added.contains(code)) {
                result.add(cap);
                added.add(code);
            }
        }

        return result;
    }

    /**
     * 合并参数：defaultParamsJson + hintParams（hint 覆盖同名键）
     */
    private Map<String, Object> mergeParams(String defaultParamsJson, Map<String, Object> hintParams) {
        Map<String, Object> params = new HashMap<>();

        // 先解析 defaultParamsJson
        if (defaultParamsJson != null && !defaultParamsJson.trim().isEmpty()) {
            try {
                Map<String, Object> defaultParams = objectMapper.readValue(
                        defaultParamsJson,
                        new TypeReference<Map<String, Object>>() {}
                );
                params.putAll(defaultParams);
            } catch (Exception e) {
                // JSON 解析失败，忽略
            }
        }

        // 然后合并 hintParams（覆盖同名键）
        if (hintParams != null) {
            params.putAll(hintParams);
        }

        return params;
    }

    /**
     * 获取 API Key 认证信息
     * @param providerId Provider ID
     * @return Auth 对象
     * @throws NoActiveApiKeyException 如果找不到可用的 API Key
     */
    private AiResolveResponse.Auth getApiKeyAuth(Long providerId) {
        // 查询 provider_api_keys(status='ACTIVE') 取最新的一条
        ProviderApiKey apiKey = apiKeyRepository
                .findFirstByProviderIdAndStatusOrderByCreatedAtDesc(providerId, "ACTIVE")
                .orElseThrow(() -> {
                    log.warn("No active API key found for provider ID: {}", providerId);
                    return new NoActiveApiKeyException("No active API key found for provider");
                });

        // 解密 api_key_cipher
        String decryptedApiKey;
        try {
            decryptedApiKey = cryptoUtil.decrypt(apiKey.getApiKeyCipher());
        } catch (Exception e) {
            log.error("Failed to decrypt API key for provider ID: {}", providerId, e);
            throw new RuntimeException("Failed to decrypt API key", e);
        }

        // 构建 Auth 对象（不记录明文到日志）
        AiResolveResponse.Auth auth = new AiResolveResponse.Auth();
        auth.setType("api_key");
        auth.setApiKey(decryptedApiKey);

        return auth;
    }
}
