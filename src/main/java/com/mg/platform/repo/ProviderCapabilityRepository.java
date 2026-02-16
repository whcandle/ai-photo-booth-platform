package com.mg.platform.repo;

import com.mg.platform.domain.ProviderCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderCapabilityRepository extends JpaRepository<ProviderCapability, Long> {
    List<ProviderCapability> findByCapabilityAndStatusOrderByPriorityAsc(String capability, String status);
    List<ProviderCapability> findByProviderId(Long providerId);
    Optional<ProviderCapability> findByProviderIdAndCapability(Long providerId, String capability);
}
