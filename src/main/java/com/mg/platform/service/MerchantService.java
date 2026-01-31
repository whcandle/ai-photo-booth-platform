package com.mg.platform.service;

import com.mg.platform.common.dto.PageResponse;
import com.mg.platform.domain.Activity;
import com.mg.platform.domain.ActivityTemplate;
import com.mg.platform.domain.Device;
import com.mg.platform.domain.DeviceActivityAssignment;
import com.mg.platform.domain.Merchant;
import com.mg.platform.domain.Template;
import com.mg.platform.domain.TemplateVersion;
import com.mg.platform.repo.ActivityRepository;
import com.mg.platform.repo.ActivityTemplateRepository;
import com.mg.platform.repo.DeviceActivityAssignmentRepository;
import com.mg.platform.repo.DeviceRepository;
import com.mg.platform.repo.TemplateVersionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantService {
    private final ActivityRepository activityRepository;
    private final ActivityTemplateRepository activityTemplateRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceActivityAssignmentRepository assignmentRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public List<Activity> getMerchantActivities(Long merchantId) {
        return activityRepository.findByMerchantId(merchantId);
    }

    /**
     * 分页查询活动列表
     * @param merchantId 商户ID（必填）
     * @param q 搜索关键词（可选，对 name/description 模糊查询）
     * @param status 状态过滤（可选，ALL/ACTIVE/INACTIVE，默认 ALL）
     * @param page 页码（默认 0）
     * @param size 每页大小（默认 20）
     * @param sort 排序字段（默认 updatedAt）
     * @param direction 排序方向（默认 desc）
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResponse<ActivityListItemDto> getMerchantActivitiesPage(
            Long merchantId,
            String q,
            String status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // 参数默认值处理
        int finalPage = page < 0 ? 0 : page;
        int finalSize = size <= 0 ? 20 : (size > 100 ? 100 : size);
        String finalSort = StringUtils.hasText(sort) ? sort : "updatedAt";
        String finalDirection = StringUtils.hasText(direction) ? direction : "desc";
        String finalStatus = StringUtils.hasText(status) ? status : "ALL";
        String finalQ = q; // 保持引用不变

        // 构建排序
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(finalDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        Sort sortObj = Sort.by(sortDirection, finalSort);
        Pageable pageable = PageRequest.of(finalPage, finalSize, sortObj);

        // 构建查询条件
        Specification<Activity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // merchantId 必填条件
            predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));

            // 搜索关键词（name 或 description 模糊匹配）
            if (StringUtils.hasText(finalQ)) {
                String likePattern = "%" + finalQ + "%";
                Predicate namePredicate = cb.like(root.get("name"), likePattern);
                Predicate descPredicate = cb.like(root.get("description"), likePattern);
                predicates.add(cb.or(namePredicate, descPredicate));
            }

            // 状态过滤
            if (!"ALL".equalsIgnoreCase(finalStatus)) {
                predicates.add(cb.equal(root.get("status"), finalStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 执行分页查询
        Page<Activity> pageResult = activityRepository.findAll(spec, pageable);

        // 转换为 DTO
        List<ActivityListItemDto> items = pageResult.getContent().stream()
                .map(activity -> {
                    ActivityListItemDto dto = new ActivityListItemDto();
                    dto.setId(activity.getId());
                    dto.setName(activity.getName());
                    dto.setDescription(activity.getDescription());
                    dto.setStatus(activity.getStatus());
                    dto.setStartAt(activity.getStartAt());
                    dto.setEndAt(activity.getEndAt());
                    dto.setCreatedAt(activity.getCreatedAt());
                    dto.setUpdatedAt(activity.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponse.of(items, finalPage, finalSize, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found: " + activityId));
    }

    public Activity createActivity(Long merchantId, CreateActivityRequest request) {
        Merchant merchant = new Merchant();
        merchant.setId(merchantId);

        Activity activity = new Activity();
        activity.setMerchant(merchant);
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setStartAt(request.getStartAt());
        activity.setEndAt(request.getEndAt());
        activity.setStatus("ACTIVE");

        return activityRepository.save(activity);
    }

    /**
     * 全量覆盖绑定模板版本：
     * 前端传入当前"完整选中"的 templateVersionIds 列表，
     * 后端采用全量覆盖语义：先删除所有旧绑定，再按顺序插入新绑定。
     * 这样支持：取消绑定（提交更少的ids）、增加绑定（提交更多的ids）、不会重复插入。
     */
    @Transactional
    public void bindTemplateVersionsToActivity(Long activityId, List<Long> templateVersionIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 1) 先删除该活动的所有旧绑定（全量覆盖语义）
        activityTemplateRepository.deleteByActivityId(activityId);

        // 2) 如果传入空列表，表示全部取消绑定，直接返回
        if (templateVersionIds == null || templateVersionIds.isEmpty()) {
            return;
        }

        // 3) 去重并保持前端顺序
        Set<Long> desiredIds = new LinkedHashSet<>(templateVersionIds);

        // 4) 按顺序批量插入新绑定
        int sortOrder = 0;
        for (Long versionId : desiredIds) {
            TemplateVersion templateVersion = templateVersionRepository.findById(versionId)
                    .orElseThrow(() -> new RuntimeException("TemplateVersion not found: " + versionId));

            ActivityTemplate at = new ActivityTemplate();
            at.setActivity(activity);
            at.setTemplateVersion(templateVersion); // 自动同步 template 字段
            at.setIsEnabled(true);
            at.setSortOrder(sortOrder++);
            activityTemplateRepository.save(at);
        }
    }

    /**
     * 根据模板ID列表，找到每个模板的最新版本ID，然后绑定到活动
     * 用于兼容旧接口：/activities/{id}/templates
     */
    @Transactional
    public void bindTemplatesToActivity(Long activityId, List<Long> templateIds) {
        // 将 templateIds 转换为对应的最新 templateVersionIds
        List<Long> templateVersionIds = templateIds.stream()
                .map(templateId -> {
                    // 找到该模板下状态为 ACTIVE 的版本，按 id 降序取第一个（最新）
                    List<TemplateVersion> versions = templateVersionRepository.findByTemplateIdAndStatus(templateId, "ACTIVE");
                    if (versions.isEmpty()) {
                        throw new RuntimeException("No active template version found for template: " + templateId);
                    }
                    // 按 id 降序排序，取最新的
                    return versions.stream()
                            .max((v1, v2) -> Long.compare(v1.getId(), v2.getId()))
                            .map(TemplateVersion::getId)
                            .orElseThrow(() -> new RuntimeException("No template version found for template: " + templateId));
                })
                .collect(Collectors.toList());

        // 调用新方法
        bindTemplateVersionsToActivity(activityId, templateVersionIds);
    }

    /**
     * 同步绑定设备到活动（diff模式）：
     * 前端传入当前"完整选中"的 deviceIds 列表，
     * 后端按差异进行删除（软删除）/ 新增 / 恢复，避免重复记录。
     */
    @Transactional
    public void bindDevicesToActivity(Long activityId, List<Long> deviceIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 空列表视为"全部取消绑定"
        if (deviceIds == null || deviceIds.isEmpty()) {
            List<DeviceActivityAssignment> existing = assignmentRepository.findByActivityIdAndStatus(activityId, "ACTIVE");
            for (DeviceActivityAssignment assignment : existing) {
                assignment.setStatus("INACTIVE");
                assignment.setDeactivatedAt(java.time.LocalDateTime.now());
                assignmentRepository.save(assignment);
            }
            return;
        }

        // 去重并保持前端顺序
        Set<Long> desiredIds = new LinkedHashSet<>(deviceIds);

        // 1) 加载现有 ACTIVE 绑定
        List<DeviceActivityAssignment> existing = assignmentRepository.findByActivityIdAndStatus(activityId, "ACTIVE");

        // 2) 取消不再需要的绑定（软删除）
        for (DeviceActivityAssignment assignment : existing) {
            Long deviceId = assignment.getDevice().getId();
            if (!desiredIds.contains(deviceId)) {
                assignment.setStatus("INACTIVE");
                assignment.setDeactivatedAt(java.time.LocalDateTime.now());
                assignmentRepository.save(assignment);
            }
        }

        // 3) 新增或恢复需要的绑定
        for (Long deviceId : desiredIds) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

            // 查找是否已存在绑定（可能是 INACTIVE 状态）
            java.util.Optional<DeviceActivityAssignment> existingAssignment = 
                    assignmentRepository.findByActivityIdAndDeviceId(activityId, deviceId);

            DeviceActivityAssignment assignment;
            if (existingAssignment.isPresent()) {
                // 恢复已存在的绑定
                assignment = existingAssignment.get();
                assignment.setStatus("ACTIVE");
                assignment.setActivatedAt(java.time.LocalDateTime.now());
                assignment.setDeactivatedAt(null);
            } else {
                // 创建新绑定
                assignment = new DeviceActivityAssignment();
                assignment.setDevice(device);
                assignment.setActivity(activity);
                assignment.setStatus("ACTIVE");
                assignment.setActivatedAt(java.time.LocalDateTime.now());
            }
            assignmentRepository.save(assignment);
        }
    }

    /**
     * 查询某个活动当前已绑定的设备 ID 列表（仅 ACTIVE 状态）
     */
    @Transactional(readOnly = true)
    public List<Long> getActivityDeviceIds(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        return assignmentRepository.findByActivityIdAndStatus(activityId, "ACTIVE")
                .stream()
                .map(assignment -> assignment.getDevice().getId())
                .collect(Collectors.toList());
    }

    public List<Device> getMerchantDevices(Long merchantId) {
        return deviceRepository.findByMerchantId(merchantId);
    }

    public Device createDevice(Long merchantId, CreateDeviceRequest request) {
        Merchant merchant = new Merchant();
        merchant.setId(merchantId);

        Device device = new Device();
        device.setMerchant(merchant);
        device.setDeviceCode(request.getDeviceCode());
        device.setName(request.getName());
        device.setStatus("ACTIVE");

        return deviceRepository.save(device);
    }

    /**
     * 重置某个活动的模板绑定（删除所有 ActivityTemplate 记录）
     */
    @Transactional
    public void resetActivityTemplateBindings(Long activityId) {
        // 确认活动存在，避免静默删除错误 ID
        activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        activityTemplateRepository.deleteByActivityId(activityId);
    }

    /**
     * 查询某个活动当前已绑定的模板版本 ID 列表（仅 is_enabled = true）
     */
    @Transactional(readOnly = true)
    public List<Long> getActivityTemplateVersionIds(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        return activityTemplateRepository.findByActivityIdAndIsEnabledTrue(activityId)
                .stream()
                .map(at -> at.getTemplateVersion().getId())
                .collect(Collectors.toList());
    }

    /**
     * 查询某个活动已绑定的模板版本详细信息列表（按 sortOrder 排序）
     * 返回包含 templateVersionId, versionSemver, templateId, templateName, coverUrl, sortOrder
     */
    @Transactional(readOnly = true)
    public List<ActivityBoundTemplateVersionDto> getActivityBoundTemplateVersions(Long activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 使用 JOIN FETCH 查询，避免 N+1 问题，并已按 sortOrder 排序
        return activityTemplateRepository.findByActivityIdAndIsEnabledTrueWithDetails(activityId)
                .stream()
                .map(at -> {
                    TemplateVersion tv = at.getTemplateVersion();
                    Template t = tv.getTemplate();
                    
                    ActivityBoundTemplateVersionDto dto = new ActivityBoundTemplateVersionDto();
                    dto.setTemplateVersionId(tv.getId());
                    dto.setVersionSemver(tv.getVersion());
                    dto.setTemplateId(t.getId());
                    dto.setTemplateName(t.getName());
                    dto.setCoverUrl(t.getCoverUrl());
                    dto.setSortOrder(at.getSortOrder() != null ? at.getSortOrder() : 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可用的模板版本列表（仅 ACTIVE 状态）
     * 返回扁平列表，包含模板和版本信息
     */
    @Transactional(readOnly = true)
    public List<TemplateVersionInfo> getAvailableTemplateVersions() {
        List<TemplateVersion> versions = templateVersionRepository.findAll().stream()
                .filter(tv -> "ACTIVE".equals(tv.getStatus()))
                .filter(tv -> tv.getTemplate() != null && "ACTIVE".equals(tv.getTemplate().getStatus()))
                .collect(Collectors.toList());

        return versions.stream()
                .map(tv -> {
                    Template t = tv.getTemplate();
                    TemplateVersionInfo info = new TemplateVersionInfo();
                    info.setTemplateVersionId(tv.getId());
                    info.setVersionSemver(tv.getVersion());
                    info.setPackageUrl(tv.getPackageUrl());
                    info.setChecksum(tv.getChecksum());
                    info.setTemplateId(t.getId());
                    info.setTemplateName(t.getName());
                    info.setCoverUrl(t.getCoverUrl());
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Data
    public static class CreateActivityRequest {
        private String name;
        private String description;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;
    }

    @Data
    public static class CreateDeviceRequest {
        private String deviceCode;
        private String name;
    }

    @Data
    public static class TemplateVersionInfo {
        private Long templateVersionId;
        private String versionSemver;
        private String packageUrl;
        private String checksum;
        private Long templateId;
        private String templateName;
        private String coverUrl;
    }

    @Data
    public static class ActivityListItemDto {
        private Long id;
        private String name;
        private String description;
        private String status;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    @Data
    public static class ActivityBoundTemplateVersionDto {
        private Long templateVersionId;
        private String versionSemver;
        private Long templateId;
        private String templateName;
        private String coverUrl;
        private Integer sortOrder;
    }
}
