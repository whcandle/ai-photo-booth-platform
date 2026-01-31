package com.mg.platform.service;

import com.mg.platform.common.util.JwtUtil;
import com.mg.platform.domain.Activity;
import com.mg.platform.domain.ActivityTemplate;
import com.mg.platform.domain.Device;
import com.mg.platform.domain.DeviceActivityAssignment;
import com.mg.platform.domain.Template;
import com.mg.platform.domain.TemplateVersion;
import com.mg.platform.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

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
        // 查询所有 activity_templates，返回 enabled 字段
        List<ActivityTemplate> activityTemplates =
                activityTemplateRepository.findByActivityId(activityId);

        return activityTemplates.stream()
                .filter(at -> "ACTIVE".equals(at.getTemplate().getStatus()))
                .map(at -> {
                    // 直接从 ActivityTemplate 获取绑定的 TemplateVersion
                    TemplateVersion tv = at.getTemplateVersion();
                    Template t = tv.getTemplate();

                    return new TemplateInfo(
                            t.getId(),                     // templateId
                            t.getName(),                   // name
                            t.getCoverUrl(),               // coverUrl
                            tv.getVersion(),               // version
                            tv.getPackageUrl(),            // downloadUrl
                            tv.getChecksum(),              // checksum
                            at.getIsEnabled()              // enabled (from activity_templates.is_enabled)
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public HandshakeResponse handshake(String deviceCode, String secret) {
        // Find device by deviceCode
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Verify device status
        if (!"ACTIVE".equals(device.getStatus())) {
            throw new RuntimeException("Device is not active");
        }

        // Verify secret
        if (device.getSecret() == null || !device.getSecret().equals(secret)) {
            throw new RuntimeException("Invalid secret");
        }

        // Update last seen time
        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        // Generate device token
        String deviceToken = jwtUtil.generateDeviceToken(device.getId(), device.getMerchant().getId());

        // Calculate expiration in seconds
        long expiresIn = jwtExpiration / 1000; // Convert milliseconds to seconds

        // Get server time in ISO 8601 format with timezone
        ZonedDateTime serverTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        String serverTimeStr = serverTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new HandshakeResponse(
                device.getId(),
                deviceToken,
                expiresIn,
                serverTimeStr
        );
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
        private Long templateId;
        private String name;
        private String coverUrl;
        private String version;
        private String downloadUrl;
        private String checksum;
        private Boolean enabled;

        public TemplateInfo(Long templateId, String name, String coverUrl,
                           String version, String downloadUrl, String checksum,
                           Boolean enabled) {
            this.templateId = templateId;
            this.name = name;
            this.coverUrl = coverUrl;
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.checksum = checksum;
            this.enabled = enabled;
        }

        // Getters
        public Long getTemplateId() { return templateId; }
        public String getName() { return name; }
        public String getCoverUrl() { return coverUrl; }
        public String getVersion() { return version; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getChecksum() { return checksum; }
        public Boolean getEnabled() { return enabled; }
    }

    public static class HandshakeResponse {
        private Long deviceId;
        private String deviceToken;
        private Long expiresIn;
        private String serverTime;

        public HandshakeResponse(Long deviceId, String deviceToken, Long expiresIn, String serverTime) {
            this.deviceId = deviceId;
            this.deviceToken = deviceToken;
            this.expiresIn = expiresIn;
            this.serverTime = serverTime;
        }

        public Long getDeviceId() { return deviceId; }
        public String getDeviceToken() { return deviceToken; }
        public Long getExpiresIn() { return expiresIn; }
        public String getServerTime() { return serverTime; }
    }
}
