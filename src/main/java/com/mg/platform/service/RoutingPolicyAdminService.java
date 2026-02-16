package com.mg.platform.service;

import com.mg.platform.domain.CapabilityRoutingPolicy;
import com.mg.platform.domain.Merchant;
import com.mg.platform.repo.CapabilityRoutingPolicyRepository;
import com.mg.platform.repo.MerchantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingPolicyAdminService {
    private final CapabilityRoutingPolicyRepository policyRepository;
    private final MerchantRepository merchantRepository;

    public List<CapabilityRoutingPolicy> listPolicies() {
        return policyRepository.findAll();
    }

    @Transactional
    public CapabilityRoutingPolicy createPolicy(CreatePolicyRequest request) {
        // 验证 merchant（如果提供了 merchantCode）
        Merchant merchant = null;
        if (request.getMerchantCode() != null && !request.getMerchantCode().trim().isEmpty()) {
            merchant = merchantRepository.findByCode(request.getMerchantCode())
                    .orElseThrow(() -> new RuntimeException("Merchant not found: " + request.getMerchantCode()));
        }

        // 检查是否已存在相同的 policy
        Long merchantId = merchant != null ? merchant.getId() : null;
        if (policyRepository.findByScopeAndMerchantIdAndCapability(
                request.getScope(), merchantId, request.getCapability()).isPresent()) {
            throw new RuntimeException("Policy already exists for scope=" + request.getScope() +
                    ", merchantId=" + merchantId + ", capability=" + request.getCapability());
        }

        CapabilityRoutingPolicy policy = new CapabilityRoutingPolicy();
        policy.setScope(request.getScope() != null ? request.getScope() : "GLOBAL");
        policy.setMerchant(merchant);
        policy.setCapability(request.getCapability());
        policy.setPreferProvidersJson(request.getPreferProvidersJson());
        policy.setRetryCount(request.getRetryCount() != null ? request.getRetryCount() : 0);
        policy.setFailoverOnHttpCodesJson(request.getFailoverOnHttpCodesJson());
        policy.setMaxCostTier(request.getMaxCostTier());
        policy.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return policyRepository.save(policy);
    }

    @Transactional
    public CapabilityRoutingPolicy updatePolicy(Long policyId, UpdatePolicyRequest request) {
        CapabilityRoutingPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        if (request.getPreferProvidersJson() != null) {
            policy.setPreferProvidersJson(request.getPreferProvidersJson());
        }
        if (request.getRetryCount() != null) {
            policy.setRetryCount(request.getRetryCount());
        }
        if (request.getFailoverOnHttpCodesJson() != null) {
            policy.setFailoverOnHttpCodesJson(request.getFailoverOnHttpCodesJson());
        }
        if (request.getMaxCostTier() != null) {
            policy.setMaxCostTier(request.getMaxCostTier());
        }
        if (request.getStatus() != null) {
            policy.setStatus(request.getStatus());
        }
        // scope, merchant, capability 不允许修改

        return policyRepository.save(policy);
    }

    @Data
    public static class CreatePolicyRequest {
        private String scope; // GLOBAL or MERCHANT
        private String merchantCode; // 如果 scope=MERCHANT，必须提供
        private String capability;
        private String preferProvidersJson;
        private Integer retryCount;
        private String failoverOnHttpCodesJson;
        private Integer maxCostTier;
        private String status;
    }

    @Data
    public static class UpdatePolicyRequest {
        private String preferProvidersJson;
        private Integer retryCount;
        private String failoverOnHttpCodesJson;
        private Integer maxCostTier;
        private String status;
    }
}
