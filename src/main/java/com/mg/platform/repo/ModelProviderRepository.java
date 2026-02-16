package com.mg.platform.repo;

import com.mg.platform.domain.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {
    Optional<ModelProvider> findByCode(String code);
}
