package com.mg.platform.repo;

import com.mg.platform.domain.ProviderApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderApiKeyRepository extends JpaRepository<ProviderApiKey, Long> {
    List<ProviderApiKey> findByProviderIdAndStatus(Long providerId, String status);
    Optional<ProviderApiKey> findFirstByProviderIdAndStatusOrderByCreatedAtDesc(Long providerId, String status);
}
