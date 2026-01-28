package com.mg.platform.service;

import com.mg.platform.domain.Activity;
import com.mg.platform.domain.ActivityTemplate;
import com.mg.platform.domain.Device;
import com.mg.platform.domain.DeviceActivityAssignment;
import com.mg.platform.domain.Merchant;
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

import java.util.List;
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

    @Transactional
    public void bindTemplateVersionsToActivity(Long activityId, List<Long> templateVersionIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 删除现有绑定
        activityTemplateRepository.deleteByActivityId(activityId);

        // 添加新绑定
        for (int i = 0; i < templateVersionIds.size(); i++) {
            Long templateVersionId = templateVersionIds.get(i);
            TemplateVersion templateVersion = templateVersionRepository.findById(templateVersionId)
                    .orElseThrow(() -> new RuntimeException("TemplateVersion not found: " + templateVersionId));

            ActivityTemplate at = new ActivityTemplate();
            at.setActivity(activity);
            at.setTemplateVersion(templateVersion); // 会自动同步冗余 template 字段
            at.setSortOrder(i);
            at.setIsEnabled(true);
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
}
