package com.mg.platform.web.merchant;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.common.dto.PageResponse;
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

    /**
     * 分页查询活动列表
     * @param merchantId 商户ID（必填）
     * @param q 搜索关键词（可选，对 name/description 模糊查询）
     * @param status 状态过滤（可选，ALL/ACTIVE/INACTIVE，默认 ALL）
     * @param page 页码（默认 0）
     * @param size 每页大小（默认 20）
     * @param sort 排序字段（默认 updatedAt）
     * @param direction 排序方向（默认 desc）
     */
    @GetMapping("/activities")
    public ApiResponse<PageResponse<MerchantService.ActivityListItemDto>> getActivities(
            @RequestParam Long merchantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction
    ) {
        try {
            PageResponse<MerchantService.ActivityListItemDto> result = merchantService.getMerchantActivitiesPage(
                    merchantId, q, status, page, size, sort, direction);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<Activity> getActivity(@PathVariable Long activityId) {
        try {
            Activity activity = merchantService.getActivityById(activityId);
            return ApiResponse.success(activity);
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

    // 旧接口：兼容传入 templateIds，内部会找到每个模板的最新版本
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

    // 新接口：按模板版本绑定
    @PostMapping("/activities/{activityId}/template-versions")
    public ApiResponse<String> bindTemplateVersions(
            @PathVariable Long activityId,
            @RequestBody BindTemplateVersionsRequest request
    ) {
        try {
            merchantService.bindTemplateVersionsToActivity(activityId, request.getTemplateVersionIds());
            return ApiResponse.success("Template versions bound successfully");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询某个活动当前已绑定的模板版本 ID 列表（用于前端回显）
     * 保留此接口以兼容前端现有代码
     */
    @GetMapping("/activities/{activityId}/template-versions/ids")
    public ApiResponse<List<Long>> getActivityTemplateVersionIds(@PathVariable Long activityId) {
        try {
            List<Long> ids = merchantService.getActivityTemplateVersionIds(activityId);
            return ApiResponse.success(ids);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询某个活动已绑定的模板版本详细信息列表（按 sortOrder 排序）
     * 返回包含 templateVersionId, versionSemver, templateId, templateName, coverUrl, sortOrder
     */
    @GetMapping("/activities/{activityId}/template-versions")
    public ApiResponse<List<MerchantService.ActivityBoundTemplateVersionDto>> getActivityBoundTemplateVersions(
            @PathVariable Long activityId
    ) {
        try {
            List<MerchantService.ActivityBoundTemplateVersionDto> result = 
                    merchantService.getActivityBoundTemplateVersions(activityId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 重置某个活动的模板绑定（删除所有已绑定的模板版本）
     */
    @PostMapping("/activities/{activityId}/template-versions/reset")
    public ApiResponse<String> resetTemplateVersions(@PathVariable Long activityId) {
        try {
            merchantService.resetActivityTemplateBindings(activityId);
            return ApiResponse.success("Activity template bindings reset successfully");
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

    @GetMapping("/activities/{activityId}/devices")
    public ApiResponse<List<Long>> getActivityDevices(@PathVariable Long activityId) {
        try {
            List<Long> deviceIds = merchantService.getActivityDeviceIds(activityId);
            return ApiResponse.success(deviceIds);
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

    @GetMapping("/template-versions")
    public ApiResponse<List<MerchantService.TemplateVersionInfo>> getTemplateVersions() {
        try {
            List<MerchantService.TemplateVersionInfo> versions = merchantService.getAvailableTemplateVersions();
            return ApiResponse.success(versions);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    static class CreateActivityRequest extends MerchantService.CreateActivityRequest {
        private Long merchantId;
    }

    @Data
    static class BindTemplatesRequest {
        private List<Long> templateIds;
    }

    @Data
    static class BindTemplateVersionsRequest {
        private List<Long> templateVersionIds;
    }

    @Data
    static class BindDevicesRequest {
        private List<Long> deviceIds;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    static class CreateDeviceRequest extends MerchantService.CreateDeviceRequest {
        private Long merchantId;
    }
}
