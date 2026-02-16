# 设备端模板 API 测试指南

## 测试场景

### 1. 字段存在性和非空验证

测试 `GET /api/v1/device/{deviceId}/activities/{activityId}/templates` 返回的 DTO 字段：

```java
// 示例测试代码（JUnit 5 + Mockito）
@Test
void testGetActivityTemplates_NewFieldsExist() {
    // Arrange
    Long deviceId = 1L;
    Long activityId = 1L;
    
    // Mock data
    Template template = new Template();
    template.setId(1L);
    template.setCode("tpl_001");  // 必需字段
    template.setName("测试模板");
    template.setCoverUrl("https://example.com/cover.png");
    template.setStatus("ACTIVE");
    
    TemplateVersion templateVersion = new TemplateVersion();
    templateVersion.setVersion("0.1.0");  // semver 格式
    templateVersion.setPackageUrl("https://example.com/package.zip");
    templateVersion.setChecksum("a1b2c3d4e5f6...");
    templateVersion.setTemplate(template);
    
    ActivityTemplate activityTemplate = new ActivityTemplate();
    activityTemplate.setTemplate(template);
    activityTemplate.setTemplateVersion(templateVersion);
    activityTemplate.setIsEnabled(true);
    
    // Act
    List<DeviceService.TemplateInfo> templates = deviceService.getActivityTemplates(deviceId, activityId);
    
    // Assert
    assertThat(templates).isNotEmpty();
    DeviceService.TemplateInfo info = templates.get(0);
    
    // 新字段必须存在且非空
    assertThat(info.getTemplateCode()).isNotNull().isNotBlank();
    assertThat(info.getVersionSemver()).isNotNull().isNotBlank();
    assertThat(info.getChecksumSha256()).isNotNull().isNotBlank();
    assertThat(info.getDownloadUrl()).isNotNull().isNotBlank();
    
    // 验证字段值来源
    assertThat(info.getTemplateCode()).isEqualTo("tpl_001");
    assertThat(info.getVersionSemver()).isEqualTo("0.1.0");
    assertThat(info.getChecksumSha256()).isEqualTo("a1b2c3d4e5f6...");
    assertThat(info.getDownloadUrl()).isEqualTo("https://example.com/package.zip");
    assertThat(info.getEnabled()).isTrue();
    
    // Deprecated 字段仍存在（向后兼容）
    assertThat(info.getTemplateId()).isNotNull();
    assertThat(info.getVersion()).isNotNull();
}
```

### 2. Template.code 非空验证

测试当 `Template.code` 为空时抛出异常：

```java
@Test
void testGetActivityTemplates_TemplateCodeRequired() {
    // Arrange
    Long deviceId = 1L;
    Long activityId = 1L;
    
    Template template = new Template();
    template.setId(1L);
    template.setCode(null);  // 空值，应抛出异常
    template.setStatus("ACTIVE");
    
    TemplateVersion templateVersion = new TemplateVersion();
    templateVersion.setTemplate(template);
    
    ActivityTemplate activityTemplate = new ActivityTemplate();
    activityTemplate.setTemplate(template);
    activityTemplate.setTemplateVersion(templateVersion);
    
    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        deviceService.getActivityTemplates(deviceId, activityId);
    });
    
    assertThat(exception.getMessage())
        .contains("Template.code is required for device output");
}
```

### 3. 集成测试（REST API）

使用 `curl` 或 Postman 测试实际 API：

```bash
# 1. 先进行 handshake 获取 deviceToken
curl -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{
    "deviceCode": "dev_001",
    "secret": "your_secret"
  }'

# 2. 使用 deviceToken 获取模板列表
curl -X GET "http://localhost:8080/api/v1/device/1/activities/1/templates" \
  -H "Authorization: Bearer <deviceToken>"

# 预期响应：
# {
#   "ok": true,
#   "data": [
#     {
#       "templateCode": "tpl_001",
#       "versionSemver": "0.1.0",
#       "checksumSha256": "...",
#       "downloadUrl": "...",
#       "enabled": true,
#       "updatedAt": "2026-02-04T12:00:00Z",
#       "name": "...",
#       "coverUrl": "...",
#       "templateId": 1,
#       "version": "0.1.0"
#     }
#   ]
# }
```

### 4. JSON 字段验证

验证 JSON 序列化后的字段名：

```java
@Test
void testTemplateInfo_JsonSerialization() throws Exception {
    // Arrange
    DeviceService.TemplateInfo info = new DeviceService.TemplateInfo(
        1L,                    // templateId
        "tpl_001",            // templateCode
        "测试模板",            // name
        "https://...",        // coverUrl
        "0.1.0",             // version (deprecated)
        "0.1.0",             // versionSemver
        "https://...",       // downloadUrl
        "a1b2c3...",         // checksumSha256
        true,                // enabled
        "2026-02-04T12:00:00Z"  // updatedAt
    );
    
    // Act
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(info);
    JsonNode node = mapper.readTree(json);
    
    // Assert - 验证新字段存在
    assertThat(node.has("templateCode")).isTrue();
    assertThat(node.has("versionSemver")).isTrue();
    assertThat(node.has("checksumSha256")).isTrue();  // 注意：字段名是 checksumSha256
    
    // Assert - 验证 deprecated 字段仍存在
    assertThat(node.has("templateId")).isTrue();
    assertThat(node.has("version")).isTrue();
    
    // Assert - 验证字段值
    assertThat(node.get("templateCode").asText()).isEqualTo("tpl_001");
    assertThat(node.get("versionSemver").asText()).isEqualTo("0.1.0");
    assertThat(node.get("checksumSha256").asText()).isEqualTo("a1b2c3...");
}
```

## 测试检查清单

- [ ] `templateCode` 字段存在且非空
- [ ] `versionSemver` 字段存在且非空
- [ ] `checksumSha256` 字段存在且非空（注意字段名）
- [ ] `downloadUrl` 字段存在且非空
- [ ] `enabled` 字段存在
- [ ] `updatedAt` 字段存在且为 ISO8601 格式
- [ ] `templateCode` 来自 `Template.code`
- [ ] `versionSemver` 来自 `TemplateVersion.version`
- [ ] `checksumSha256` 来自 `TemplateVersion.checksum`
- [ ] `downloadUrl` 来自 `TemplateVersion.packageUrl`
- [ ] `enabled` 来自 `ActivityTemplate.isEnabled`
- [ ] 当 `Template.code` 为空时抛出异常
- [ ] `templateId` 和 `version` 字段仍存在（向后兼容）
- [ ] JSON 序列化字段名正确（camelCase）

## 注意事项

1. **字段名大小写**：所有字段使用 camelCase，JSON 序列化会自动处理
2. **checksumSha256 字段名**：使用 `@JsonProperty("checksumSha256")` 确保序列化字段名正确
3. **向后兼容**：deprecated 字段（`templateId`、`version`）在响应中仍存在，但不应作为主要字段使用
4. **异常处理**：`Template.code` 为空时会抛出 `RuntimeException`，应确保所有模板都有 code
