package com.mg.platform.repo;

import com.mg.platform.domain.ActivityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 查询活动已绑定的模板版本（启用状态），使用 JOIN FETCH 避免 N+1 问题
     * 按 sortOrder 升序排序
     */
    @Query("SELECT at FROM ActivityTemplate at " +
           "JOIN FETCH at.templateVersion tv " +
           "JOIN FETCH tv.template t " +
           "WHERE at.activity.id = :activityId AND at.isEnabled = true " +
           "ORDER BY at.sortOrder ASC")
    List<ActivityTemplate> findByActivityIdAndIsEnabledTrueWithDetails(@Param("activityId") Long activityId);
}
