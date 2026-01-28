package com.mg.platform.service;

import com.mg.platform.domain.Activity;
import com.mg.platform.domain.ActivityTemplate;
import com.mg.platform.domain.Device;
import com.mg.platform.domain.DeviceActivityAssignment;
import com.mg.platform.domain.Template;
import com.mg.platform.domain.TemplateVersion;
import com.mg.platform.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
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

        // 获取活动的模板（已绑定到具体 TemplateVersion）
        List<ActivityTemplate> activityTemplates =
                activityTemplateRepository.findByActivityIdAndIsEnabledTrue(activityId);

        return activityTemplates.stream()
                .filter(at -> "ACTIVE".equals(at.getTemplate().getStatus()))
                .map(at -> {
                    // 直接从 ActivityTemplate 获取绑定的 TemplateVersion
                    TemplateVersion tv = at.getTemplateVersion();
                    Template t = tv.getTemplate();

                    return new TemplateInfo(
                            tv.getId(),                    // templateVersionId
                            t.getId(),                     // templateId
                            t.getCode(),                  // templateCode
                            t.getName(),                  // templateName
                            t.getCoverUrl(),              // coverUrl
                            tv.getVersion(),              // versionSemver
                            tv.getPackageUrl(),           // packageUrl
                            tv.getChecksum()              // checksum
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
        private Long templateVersionId;
        private Long templateId;
        private String templateCode;
        private String templateName;
        private String coverUrl;
        private String versionSemver;
        private String packageUrl;
        private String checksum;

        public TemplateInfo(Long templateVersionId, Long templateId, String templateCode,
                           String templateName, String coverUrl, String versionSemver,
                           String packageUrl, String checksum) {
            this.templateVersionId = templateVersionId;
            this.templateId = templateId;
            this.templateCode = templateCode;
            this.templateName = templateName;
            this.coverUrl = coverUrl;
            this.versionSemver = versionSemver;
            this.packageUrl = packageUrl;
            this.checksum = checksum;
        }

        // Getters
        public Long getTemplateVersionId() { return templateVersionId; }
        public Long getTemplateId() { return templateId; }
        public String getTemplateCode() { return templateCode; }
        public String getTemplateName() { return templateName; }
        public String getCoverUrl() { return coverUrl; }
        public String getVersionSemver() { return versionSemver; }
        public String getPackageUrl() { return packageUrl; }
        public String getChecksum() { return checksum; }
    }
}
