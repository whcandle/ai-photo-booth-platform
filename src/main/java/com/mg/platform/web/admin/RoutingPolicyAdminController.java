package com.mg.platform.web.admin;

import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.domain.CapabilityRoutingPolicy;
import com.mg.platform.service.RoutingPolicyAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/routing-policies")
@RequiredArgsConstructor
public class RoutingPolicyAdminController {
    private final RoutingPolicyAdminService routingPolicyAdminService;

    @GetMapping
    public ApiResponse<List<CapabilityRoutingPolicy>> listPolicies() {
        try {
            List<CapabilityRoutingPolicy> policies = routingPolicyAdminService.listPolicies();
            return ApiResponse.success(policies);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<CapabilityRoutingPolicy> createPolicy(
            @RequestBody RoutingPolicyAdminService.CreatePolicyRequest request
    ) {
        try {
            CapabilityRoutingPolicy policy = routingPolicyAdminService.createPolicy(request);
            return ApiResponse.success(policy);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{policyId}")
    public ApiResponse<CapabilityRoutingPolicy> updatePolicy(
            @PathVariable Long policyId,
            @RequestBody RoutingPolicyAdminService.UpdatePolicyRequest request
    ) {
        try {
            CapabilityRoutingPolicy policy = routingPolicyAdminService.updatePolicy(policyId, request);
            return ApiResponse.success(policy);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
