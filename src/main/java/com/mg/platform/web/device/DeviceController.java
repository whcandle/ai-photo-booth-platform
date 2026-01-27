package com.mg.platform.web.device;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.domain.Activity;
import com.mg.platform.service.DeviceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @PostMapping("/heartbeat")
    public ApiResponse<Map<String, String>> heartbeat(@RequestBody HeartbeatRequest request) {
        try {
            deviceService.updateDeviceHeartbeat(request.getDeviceId(), request.getVersion());
            Map<String, String> result = new HashMap<>();
            result.put("status", "OK");
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{deviceId}/activities")
    public ApiResponse<List<ActivityDto>> getDeviceActivities(@PathVariable Long deviceId) {
        try {
            List<Activity> activities = deviceService.getDeviceActivities(deviceId);
            List<ActivityDto> dtos = activities.stream()
                    .map(a -> new ActivityDto(
                            a.getId(),
                            a.getName(),
                            a.getStatus(),
                            a.getStartAt(),
                            a.getEndAt()
                    ))
                    .toList();
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{deviceId}/activities/{activityId}/templates")
    public ApiResponse<List<DeviceService.TemplateInfo>> getActivityTemplates(
            @PathVariable Long deviceId,
            @PathVariable Long activityId
    ) {
        try {
            List<DeviceService.TemplateInfo> templates = deviceService.getActivityTemplates(deviceId, activityId);
            return ApiResponse.success(templates);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Data
    static class HeartbeatRequest {
        private Long deviceId;
        private String version;
        private String status;
    }

    @Data
    static class ActivityDto {
        private Long id;
        private String name;
        private String status;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;

        public ActivityDto(Long id, String name, String status,
                          java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }
}
