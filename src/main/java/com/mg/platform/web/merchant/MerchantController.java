package com.mg.platform.web.merchant;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.domain.Activity;
import com.mg.platform.domain.Device;
import com.mg.platform.service.MerchantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;

    @GetMapping("/activities")
    public ApiResponse<List<Activity>> getActivities(@RequestParam Long merchantId) {
        try {
            List<Activity> activities = merchantService.getMerchantActivities(merchantId);
            return ApiResponse.success(activities);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/activities")
    public ApiResponse<Activity> createActivity(@RequestBody CreateActivityRequest request) {
        try {
            Activity activity = merchantService.createActivity(request.getMerchantId(), request);
            return ApiResponse.success(activity);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/activities/{activityId}/templates")
    public ApiResponse<String> bindTemplates(
            @PathVariable Long activityId,
            @RequestBody BindTemplatesRequest request
    ) {
        try {
            merchantService.bindTemplatesToActivity(activityId, request.getTemplateIds());
            return ApiResponse.success("Templates bound successfully");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/activities/{activityId}/devices")
    public ApiResponse<String> bindDevices(
            @PathVariable Long activityId,
            @RequestBody BindDevicesRequest request
    ) {
        try {
            merchantService.bindDevicesToActivity(activityId, request.getDeviceIds());
            return ApiResponse.success("Devices bound successfully");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/devices")
    public ApiResponse<List<Device>> getDevices(@RequestParam Long merchantId) {
        try {
            List<Device> devices = merchantService.getMerchantDevices(merchantId);
            return ApiResponse.success(devices);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/devices")
    public ApiResponse<Device> createDevice(@RequestBody CreateDeviceRequest request) {
        try {
            Device device = merchantService.createDevice(request.getMerchantId(), request);
            return ApiResponse.success(device);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Data
    static class CreateActivityRequest extends MerchantService.CreateActivityRequest {
        private Long merchantId;
    }

    @Data
    static class BindTemplatesRequest {
        private List<Long> templateIds;
    }

    @Data
    static class BindDevicesRequest {
        private List<Long> deviceIds;
    }

    @Data
    static class CreateDeviceRequest extends MerchantService.CreateDeviceRequest {
        private Long merchantId;
    }
}
