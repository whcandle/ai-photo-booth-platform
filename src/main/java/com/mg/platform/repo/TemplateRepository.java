package com.mg.platform.repo;

import com.mg.platform.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByCode(String code);
    List<Template> findByStatus(String status);
}
