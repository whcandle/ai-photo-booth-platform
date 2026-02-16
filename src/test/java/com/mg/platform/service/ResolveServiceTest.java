package com.mg.platform.service;

import com.mg.platform.common.dto.AiResolveRequest;
import com.mg.platform.common.dto.AiResolveResponse;
import com.mg.platform.common.exception.NoActiveApiKeyException;
import com.mg.platform.common.util.CryptoUtil;
import com.mg.platform.domain.ModelProvider;
import com.mg.platform.domain.ProviderApiKey;
import com.mg.platform.domain.ProviderCapability;
import com.mg.platform.domain.CapabilityRoutingPolicy;
import com.mg.platform.domain.Merchant;
import com.mg.platform.repo.CapabilityRoutingPolicyRepository;
import com.mg.platform.repo.MerchantRepository;
import com.mg.platform.repo.ProviderApiKeyRepository;
import com.mg.platform.repo.ProviderCapabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveServiceTest {

    @Mock
    private ProviderCapabilityRepository capabilityRepository;

    @Mock
    private CapabilityRoutingPolicyRepository routingPolicyRepository;

    @Mock
    private ProviderApiKeyRepository apiKeyRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private CryptoUtil cryptoUtil;

    @InjectMocks
    private ResolveService resolveService;

    private ModelProvider testProvider;
    private ProviderCapability testCapability;
    private ProviderApiKey testApiKey;
    private String encryptedApiKey;
    private String decryptedApiKey;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testProvider = new ModelProvider();
        testProvider.setId(1L);
        testProvider.setCode("test_provider");
        testProvider.setName("Test Provider");
        testProvider.setStatus("ACTIVE");

        testCapability = new ProviderCapability();
        testCapability.setId(1L);
        testCapability.setProvider(testProvider);
        testCapability.setCapability("segmentation");
        testCapability.setEndpoint("https://api.test.com/v1/segmentation");
        testCapability.setStatus("ACTIVE");
        testCapability.setPriority(100);
        testCapability.setDefaultTimeoutMs(8000);
        testCapability.setDefaultParamsJson("{\"model\":\"test\"}");

        decryptedApiKey = "test-api-key-12345";
        encryptedApiKey = "encrypted-key-string";

        testApiKey = new ProviderApiKey();
        testApiKey.setId(1L);
        testApiKey.setProvider(testProvider);
        testApiKey.setName("Test API Key");
        testApiKey.setApiKeyCipher(encryptedApiKey);
        testApiKey.setStatus("ACTIVE");
    }

    @Test
    void testResolve_WithApiKey_Success() {
        // Given
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(routingPolicyRepository.findByScopeAndMerchantIdAndCapability("GLOBAL", null, "segmentation"))
                .thenReturn(Optional.empty());
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then
        assertNotNull(response);
        assertEquals("direct", response.getMode());
        assertEquals("segmentation", response.getCapability());
        assertNotNull(response.getDirect());
        assertEquals("test_provider", response.getDirect().getProviderCode());
        assertEquals("https://api.test.com/v1/segmentation", response.getDirect().getEndpoint());
        assertEquals(8000, response.getDirect().getTimeoutMs());
        assertNotNull(response.getDirect().getAuth());
        assertEquals("api_key", response.getDirect().getAuth().getType());
        assertEquals(decryptedApiKey, response.getDirect().getAuth().getApiKey());

        // 验证解密方法被调用
        verify(cryptoUtil, times(1)).decrypt(encryptedApiKey);
    }

    @Test
    void testResolve_NoApiKey_ThrowsException() {
        // Given
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(routingPolicyRepository.findByScopeAndMerchantIdAndCapability("GLOBAL", null, "segmentation"))
                .thenReturn(Optional.empty());
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.empty());

        // When & Then
        NoActiveApiKeyException exception = assertThrows(NoActiveApiKeyException.class, () -> {
            resolveService.resolve(request);
        });

        assertEquals("NO_ACTIVE_API_KEY", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("No active API key found"));

        // 验证解密方法未被调用
        verify(cryptoUtil, never()).decrypt(any());
    }

    @Test
    void testResolve_WithPrefer_Success() {
        // Given
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");
        request.setPrefer(List.of("test_provider"));

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then
        assertNotNull(response);
        assertEquals("test_provider", response.getDirect().getProviderCode());
    }

    @Test
    void testResolve_GlobalPolicy_Success() {
        // Given: 无请求 prefer，有 GLOBAL policy
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");

        // Mock GLOBAL policy
        CapabilityRoutingPolicy globalPolicy = new CapabilityRoutingPolicy();
        globalPolicy.setScope("GLOBAL");
        globalPolicy.setCapability("segmentation");
        globalPolicy.setStatus("ACTIVE");
        globalPolicy.setPreferProvidersJson("[\"test_provider\"]");

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(routingPolicyRepository.findByScopeAndMerchantIdAndCapability("GLOBAL", null, "segmentation"))
                .thenReturn(Optional.of(globalPolicy));
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then
        assertNotNull(response);
        assertEquals("test_provider", response.getDirect().getProviderCode());
    }

    @Test
    void testResolve_RequestPreferOverridesPolicy_Success() {
        // Given: 有请求 prefer 和 GLOBAL policy，请求 prefer 应该优先
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");
        request.setPrefer(List.of("volc")); // 请求 prefer volc

        // 创建另一个 provider (volc)
        ModelProvider volcProvider = new ModelProvider();
        volcProvider.setId(2L);
        volcProvider.setCode("volc");
        volcProvider.setName("Volc");
        volcProvider.setStatus("ACTIVE");

        ProviderCapability volcCapability = new ProviderCapability();
        volcCapability.setId(2L);
        volcCapability.setProvider(volcProvider);
        volcCapability.setCapability("segmentation");
        volcCapability.setEndpoint("https://api.volc.com/v1/segmentation");
        volcCapability.setStatus("ACTIVE");
        volcCapability.setPriority(200);
        volcCapability.setDefaultTimeoutMs(10000);

        // Mock GLOBAL policy 偏好 aliyun
        CapabilityRoutingPolicy globalPolicy = new CapabilityRoutingPolicy();
        globalPolicy.setScope("GLOBAL");
        globalPolicy.setCapability("segmentation");
        globalPolicy.setStatus("ACTIVE");
        globalPolicy.setPreferProvidersJson("[\"aliyun\"]");

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability, volcCapability));
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(2L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then: 应该返回 volc（请求 prefer 优先）
        assertNotNull(response);
        assertEquals("volc", response.getDirect().getProviderCode());
    }

    @Test
    void testResolve_MerchantPolicy_Success() {
        // Given: 有 merchantCode，查询 MERCHANT policy
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");
        request.setMerchantCode("TEST001");

        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setCode("TEST001");
        merchant.setName("Test Merchant");

        // Mock MERCHANT policy
        CapabilityRoutingPolicy merchantPolicy = new CapabilityRoutingPolicy();
        merchantPolicy.setScope("MERCHANT");
        merchantPolicy.setMerchant(merchant);
        merchantPolicy.setCapability("segmentation");
        merchantPolicy.setStatus("ACTIVE");
        merchantPolicy.setPreferProvidersJson("[\"test_provider\"]");

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(merchantRepository.findByCode("TEST001"))
                .thenReturn(Optional.of(merchant));
        when(routingPolicyRepository.findByScopeAndMerchantIdAndCapability("MERCHANT", 1L, "segmentation"))
                .thenReturn(Optional.of(merchantPolicy));
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then
        assertNotNull(response);
        assertEquals("test_provider", response.getDirect().getProviderCode());
    }

    @Test
    void testResolve_WithConstraintsAndHintParams_Success() {
        // Given
        AiResolveRequest request = new AiResolveRequest();
        request.setCapability("segmentation");
        AiResolveRequest.Constraints constraints = new AiResolveRequest.Constraints();
        constraints.setTimeoutMs(15000);
        request.setConstraints(constraints);
        request.setHintParams(Collections.singletonMap("quality", "high"));

        when(capabilityRepository.findByCapabilityAndStatusOrderByPriorityAsc("segmentation", "ACTIVE"))
                .thenReturn(List.of(testCapability));
        when(routingPolicyRepository.findByScopeAndMerchantIdAndCapability("GLOBAL", null, "segmentation"))
                .thenReturn(Optional.empty());
        when(apiKeyRepository.findFirstByProviderIdAndStatusOrderByCreatedAtDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(testApiKey));
        when(cryptoUtil.decrypt(encryptedApiKey)).thenReturn(decryptedApiKey);

        // When
        AiResolveResponse response = resolveService.resolve(request);

        // Then
        assertNotNull(response);
        assertEquals(15000, response.getDirect().getTimeoutMs());
        assertNotNull(response.getDirect().getParams());
        assertEquals("high", response.getDirect().getParams().get("quality"));
    }
}
