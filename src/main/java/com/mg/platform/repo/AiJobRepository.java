package com.mg.platform.repo;

import com.mg.platform.domain.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    Optional<AiJob> findById(Long id);
    List<AiJob> findByMerchantId(Long merchantId);
    List<AiJob> findByDeviceId(Long deviceId);
}
