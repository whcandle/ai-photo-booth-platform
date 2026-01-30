package com.mg.platform.repo;

import com.mg.platform.domain.ActivityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityTemplateRepository extends JpaRepository<ActivityTemplate, Long> {
    List<ActivityTemplate> findByActivityId(Long activityId);
    List<ActivityTemplate> findByActivityIdAndIsEnabledTrue(Long activityId);
    void deleteByActivityIdAndTemplateId(Long activityId, Long templateId);
    void deleteByActivityId(Long activityId);

    Optional<ActivityTemplate> findByActivityIdAndTemplateVersionId(Long activityId, Long templateVersionId);
    void deleteByActivityIdAndTemplateVersionId(Long activityId, Long templateVersionId);
}
