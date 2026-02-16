package com.mg.platform.common.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiResolveRequest {
    private String capability;
    private String templateCode;
    private String versionSemver;
    private String merchantCode;
    private List<String> prefer;
    private Constraints constraints;
    private Map<String, Object> hintParams;

    @Data
    public static class Constraints {
        private Integer timeoutMs;
        private Integer maxCostTier;
    }
}
