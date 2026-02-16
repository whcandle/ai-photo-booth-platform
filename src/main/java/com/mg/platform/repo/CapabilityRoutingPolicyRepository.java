package com.mg.platform.repo;

import com.mg.platform.domain.CapabilityRoutingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapabilityRoutingPolicyRepository extends JpaRepository<CapabilityRoutingPolicy, Long> {
    Optional<CapabilityRoutingPolicy> findByScopeAndMerchantIdAndCapability(String scope, Long merchantId, String capability);
    
    /**
     * 查找第一个匹配的 policy（按创建时间降序，获取最新的）
     * 用于处理可能存在重复记录的情况
     */
    Optional<CapabilityRoutingPolicy> findFirstByScopeAndMerchantIdAndCapabilityOrderByCreatedAtDesc(String scope, Long merchantId, String capability);
    
    List<CapabilityRoutingPolicy> findByCapabilityAndStatus(String capability, String status);
}
