package com.mg.platform.common.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiResolveResponse {
    private String mode;
    private String capability;
    private Direct direct;

    @Data
    public static class Direct {
        private String providerCode;
        private String endpoint;
        private Auth auth;
        private Integer timeoutMs;
        private Map<String, Object> params;
    }

    @Data
    public static class Auth {
        private String type;
        private String apiKey;
    }
}
