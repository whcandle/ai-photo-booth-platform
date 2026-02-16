package com.mg.platform.repo;

import com.mg.platform.domain.ActivityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 语法插入或更新活动模板绑定
     * 如果 (activity_id, template_version_id) 已存在，则更新 sort_order 和 is_enabled
     * 
     * @param activityId 活动ID
     * @param templateId 模板ID
     * @param templateVersionId 模板版本ID
     * @param sortOrder 排序顺序
     * @param isEnabled 是否启用
     */
    @Modifying
    @Query(value = "INSERT INTO activity_templates (activity_id, template_id, template_version_id, sort_order, is_enabled, created_at, updated_at) " +
                   "VALUES (:activityId, :templateId, :templateVersionId, :sortOrder, :isEnabled, NOW(), NOW()) " +
                   "ON DUPLICATE KEY UPDATE " +
                   "sort_order = VALUES(sort_order), " +
                   "is_enabled = VALUES(is_enabled), " +
                   "updated_at = NOW()",
           nativeQuery = true)
    void insertOrUpdateActivityTemplate(
            @Param("activityId") Long activityId,
            @Param("templateId") Long templateId,
            @Param("templateVersionId") Long templateVersionId,
            @Param("sortOrder") Integer sortOrder,
            @Param("isEnabled") Boolean isEnabled
    );
}
