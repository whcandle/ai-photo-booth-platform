package com.mg.platform.repo;

import com.mg.platform.domain.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {
    List<TemplateVersion> findByTemplateIdAndStatus(Long templateId, String status);
    Optional<TemplateVersion> findByTemplateIdAndVersion(Long templateId, String version);
    List<TemplateVersion> findByTemplateIdOrderByIdDesc(Long templateId);
}
