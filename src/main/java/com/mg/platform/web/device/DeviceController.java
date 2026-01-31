package com.mg.platform.web.device;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.common.util.JwtUtil;
import com.mg.platform.domain.Activity;
import com.mg.platform.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;
    private final JwtUtil jwtUtil;

    @PostMapping("/handshake")
    public ApiResponse<DeviceService.HandshakeResponse> handshake(@RequestBody HandshakeRequest request) {
        try {
            DeviceService.HandshakeResponse response = deviceService.handshake(request.getDeviceCode(), request.getSecret());
            return ApiResponse.success(response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Internal server error: " + e.getMessage());
        }
    }

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
    public ApiResponse<List<ActivityDto>> getDeviceActivities(
            @PathVariable Long deviceId,
            HttpServletRequest request
    ) {
        try {
            // Extract and validate device token
            String token = extractBearerToken(request);
            if (token == null) {
                return ApiResponse.error("Missing or invalid Authorization header");
            }

            // Validate device token and check deviceId matches
            if (!jwtUtil.validateDeviceToken(token, deviceId)) {
                return ApiResponse.error("Invalid or unauthorized device token");
            }

            // Get activities for the device
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
    public ResponseEntity<ApiResponse<List<DeviceService.TemplateInfo>>> getActivityTemplates(
            @PathVariable Long deviceId,
            @PathVariable Long activityId,
            HttpServletRequest request
    ) {
        try {
            // Extract and validate device token
            String token = extractBearerToken(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Missing or invalid Authorization header"));
            }

            // Validate device token and check deviceId matches
            if (!jwtUtil.validateDeviceToken(token, deviceId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or unauthorized device token"));
            }

            // Get templates for the activity
            // This will throw RuntimeException if device is not bound to activity
            List<DeviceService.TemplateInfo> templates = deviceService.getActivityTemplates(deviceId, activityId);
            return ResponseEntity.ok(ApiResponse.success(templates));
        } catch (RuntimeException e) {
            // Check if it's a permission error (device not bound to activity)
            if (e.getMessage() != null && e.getMessage().contains("does not have access")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Device does not have access to this activity"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    static class HandshakeRequest {
        private String deviceCode;
        private String secret;
    }

    @Data
    static class HeartbeatRequest {
        private Long deviceId;
        private String version;
        private String status;
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @Data
    static class ActivityDto {
        private Long activityId;
        private String name;
        private String status;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;

        public ActivityDto(Long activityId, String name, String status,
                          java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
            this.activityId = activityId;
            this.name = name;
            this.status = status;
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }
}
