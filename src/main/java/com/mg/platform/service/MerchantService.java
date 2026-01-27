package com.mg.platform.service;

import com.mg.platform.domain.*;
import com.mg.platform.repo.*;
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
    private final TemplateRepository templateRepository;

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
    public void bindTemplatesToActivity(Long activityId, List<Long> templateIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 删除现有绑定
        List<ActivityTemplate> existing = activityTemplateRepository.findByActivityId(activityId);
        activityTemplateRepository.deleteAll(existing);

        // 添加新绑定
        for (int i = 0; i < templateIds.size(); i++) {
            Long templateId = templateIds.get(i);
            Template template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

            ActivityTemplate at = new ActivityTemplate();
            at.setActivity(activity);
            at.setTemplate(template);
            at.setSortOrder(i);
            at.setIsEnabled(true);
            activityTemplateRepository.save(at);
        }
    }

    @Transactional
    public void bindDevicesToActivity(Long activityId, List<Long> deviceIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 先取消该活动的所有设备绑定
        List<DeviceActivityAssignment> existing = assignmentRepository.findByActivityId(activityId);
        for (DeviceActivityAssignment assignment : existing) {
            assignment.setStatus("INACTIVE");
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
