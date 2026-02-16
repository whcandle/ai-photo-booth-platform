package com.mg.platform.web.ai;

import com.mg.platform.common.dto.AiResolveRequest;
import com.mg.platform.common.dto.AiResolveResponse;
import com.mg.platform.common.dto.ApiResponse;
import com.mg.platform.common.exception.NoActiveApiKeyException;
import com.mg.platform.service.ResolveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ResolveController {
    private final ResolveService resolveService;

    @PostMapping("/resolve")
    public ResponseEntity<ApiResponse<AiResolveResponse>> resolve(@RequestBody AiResolveRequest request) {
        try {
            AiResolveResponse response = resolveService.resolve(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (NoActiveApiKeyException e) {
            // 返回 400 错误，包含错误码
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("NO_ACTIVE_API_KEY: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}
