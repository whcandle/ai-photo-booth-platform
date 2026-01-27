package com.mg.platform.service;

import com.mg.platform.domain.Activity;
import com.mg.platform.domain.Device;
import com.mg.platform.domain.DeviceActivityAssignment;
import com.mg.platform.domain.Template;
import com.mg.platform.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceActivityAssignmentRepository assignmentRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTemplateRepository activityTemplateRepository;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public List<Activity> getDeviceActivities(Long deviceId) {
        List<DeviceActivityAssignment> assignments = assignmentRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE");
        List<Long> activityIds = assignments.stream()
                .map(a -> a.getActivity().getId())
                .collect(Collectors.toList());

        List<Activity> activities = activityRepository.findAllById(activityIds);
        LocalDateTime now = LocalDateTime.now();
        return activities.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .filter(a -> a.getStartAt() == null || a.getStartAt().isBefore(now) || a.getStartAt().isEqual(now))
                .filter(a -> a.getEndAt() == null || a.getEndAt().isAfter(now) || a.getEndAt().isEqual(now))
                .collect(Collectors.toList());
    }

    public List<TemplateInfo> getActivityTemplates(Long deviceId, Long activityId) {
        // 验证设备是否有权限访问该活动
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        List<DeviceActivityAssignment> assignments = assignmentRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE");
        boolean hasAccess = assignments.stream()
                .anyMatch(a -> a.getActivity().getId().equals(activityId));

        if (!hasAccess) {
            throw new RuntimeException("Device does not have access to this activity");
        }

        // 获取活动的模板
        List<com.mg.platform.domain.ActivityTemplate> activityTemplates =
                activityTemplateRepository.findByActivityIdAndIsEnabledTrue(activityId);

        return activityTemplates.stream()
                .filter(at -> "ACTIVE".equals(at.getTemplate().getStatus()))
                .map(at -> {
                    Template template = at.getTemplate();
                    String activeVersion = templateVersionRepository.findByTemplateIdAndStatus(
                            template.getId(), "ACTIVE"
                    ).stream()
                            .findFirst()
                            .map(tv -> tv.getVersion())
                            .orElse("1.0.0");

                    return new TemplateInfo(
                            template.getId(),
                            template.getCode(),
                            template.getName(),
                            template.getCoverUrl(),
                            activeVersion,
                            templateVersionRepository.findByTemplateIdAndStatus(template.getId(), "ACTIVE")
                                    .stream()
                                    .findFirst()
                                    .map(tv -> tv.getPackageUrl())
                                    .orElse(""),
                            templateVersionRepository.findByTemplateIdAndStatus(template.getId(), "ACTIVE")
                                    .stream()
                                    .findFirst()
                                    .map(tv -> tv.getChecksum())
                                    .orElse(null),
                            true
                    );
                })
                .collect(Collectors.toList());
    }

    public void updateDeviceHeartbeat(Long deviceId, String version) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        device.setLastSeenAt(LocalDateTime.now());
        if (version != null) {
            device.setClientVersion(version);
        }
        deviceRepository.save(device);
    }

    public static class TemplateInfo {
        private Long id;
        private String templateId;
        private String name;
        private String coverUrl;
        private String activeVersion;
        private String packageUrl;
        private String checksum;
        private Boolean enabled;

        public TemplateInfo(Long id, String templateId, String name, String coverUrl,
                           String activeVersion, String packageUrl, String checksum, Boolean enabled) {
            this.id = id;
            this.templateId = templateId;
            this.name = name;
            this.coverUrl = coverUrl;
            this.activeVersion = activeVersion;
            this.packageUrl = packageUrl;
            this.checksum = checksum;
            this.enabled = enabled;
        }

        // Getters
        public Long getId() { return id; }
        public String getTemplateId() { return templateId; }
        public String getName() { return name; }
        public String getCoverUrl() { return coverUrl; }
        public String getActiveVersion() { return activeVersion; }
        public String getPackageUrl() { return packageUrl; }
        public String getChecksum() { return checksum; }
        public Boolean getEnabled() { return enabled; }
    }
}
