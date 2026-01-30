package com.mg.platform.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 同步绑定模板版本：
     * 前端传入当前“完整选中”的 templateVersionIds 列表，
     * 后端按差异进行删除 / 新增 / 更新 sort_order，避免唯一键冲突。
     */
    @Transactional
    public void bindTemplateVersionsToActivity(Long activityId, List<Long> templateVersionIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 空视为“全部取消绑定”
        if (templateVersionIds == null || templateVersionIds.isEmpty()) {
            activityTemplateRepository.deleteByActivityId(activityId);
            return;
        }

        // 去重并保持前端顺序
        Set<Long> desiredIds = new LinkedHashSet<>(templateVersionIds);

        // 1) 加载现有绑定
        List<ActivityTemplate> existing = activityTemplateRepository.findByActivityId(activityId);

        // 2) 删除不再需要的绑定
        for (ActivityTemplate at : existing) {
            Long vid = at.getTemplateVersion().getId();
            if (!desiredIds.contains(vid)) {
                activityTemplateRepository.delete(at);
            }
        }

        // 3) 新增或更新需要保留的绑定，并设置排序
        int sortOrder = 0;
        for (Long versionId : desiredIds) {
            ActivityTemplate at = activityTemplateRepository
                    .findByActivityIdAndTemplateVersionId(activityId, versionId)
                    .orElse(null);

            if (at == null) {
                TemplateVersion templateVersion = templateVersionRepository.findById(versionId)
                        .orElseThrow(() -> new RuntimeException("TemplateVersion not found: " + versionId));

                at = new ActivityTemplate();
                at.setActivity(activity);
                at.setTemplateVersion(templateVersion); // 会自动同步冗余 template 字段
                at.setIsEnabled(true);
            } else {
                // 已存在的记录，确保仍为启用状态
                at.setIsEnabled(true);
            }

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

    @Transactional
    public void bindDevicesToActivity(Long activityId, List<Long> deviceIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 先取消该活动的所有设备绑定
        List<DeviceActivityAssignment> existing = assignmentRepository.findByActivityId(activityId);
        for (DeviceActivityAssignment assignment : existing) {
            assignment.setStatus("INACTIVE");
            assignment.setDeactivatedAt(java.time.LocalDateTime.now());
            assignmentRepository.save(assignment);
        }

        // 添加新绑定
        for (Long deviceId : deviceIds) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

            DeviceActivityAssignment assignment = new DeviceActivityAssignment();
            assignment.setDevice(device);
            assignment.setActivity(activity);
            assignment.setStatus("ACTIVE");
            assignment.setActivatedAt(java.time.LocalDateTime.now());
            assignmentRepository.save(assignment);
        }
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
}
