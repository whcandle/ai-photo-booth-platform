# Templates API - updatedAt 字段新增说明

## 变更概述

为设备端模板列表接口 `GET /api/v1/device/{deviceId}/activities/{activityId}/templates` 新增 `updatedAt` 字段，用于设备端离线缓存和变更提示。

## 变更内容

### 1. TemplateInfo DTO 新增字段

**文件**: `src/main/java/com/mg/platform/service/DeviceService.java`

- **字段名**: `updatedAt`
- **类型**: `String`
- **格式**: ISO8601 (UTC)，例如：`2026-02-04T12:00:00Z`
- **可空**: 是（如果所有相关实体都没有时间戳，则为 `null`）

### 2. updatedAt 取值优先级

按照以下优先级从实体中获取 `updatedAt`：

1. `TemplateVersion.updatedAt` （最高优先级）
2. `TemplateVersion.createdAt`
3. `ActivityTemplate.updatedAt`
4. `ActivityTemplate.createdAt`
5. `Template.updatedAt`
6. `Template.createdAt` （最低优先级）

如果所有字段都为 `null`，则 `updatedAt` 返回 `null`。

### 3. 序列化格式

- 使用 UTC 时区
- ISO8601 格式：`yyyy-MM-dd'T'HH:mm:ssZ` 或 `yyyy-MM-dd'T'HH:mm:ss.SSSZ`
- 示例：`2026-02-04T12:00:00Z` 或 `2026-02-04T12:00:00.123Z`

## 向后兼容性

✅ **完全向后兼容**：
- 仅新增字段，不修改现有字段
- 不改变路由、鉴权、响应结构
- 现有客户端可以忽略新字段，不影响功能

## 测试验证

### 使用测试脚本

```powershell
# 基本用法
.\test_templates_updated_at.ps1 -deviceId 1 -activityId 1 -token "your_device_token"

# 指定平台地址
.\test_templates_updated_at.ps1 -deviceId 1 -activityId 1 -token "your_device_token" -baseUrl "http://localhost:8089"
```

### 使用 curl

```bash
curl -X GET \
  "http://127.0.0.1:8089/api/v1/device/1/activities/1/templates" \
  -H "Authorization: Bearer YOUR_DEVICE_TOKEN" \
  -H "Content-Type: application/json"
```

### 验证点

1. ✅ 响应中每个 `template` 对象都包含 `updatedAt` 字段
2. ✅ `updatedAt` 格式为 ISO8601（UTC）
3. ✅ 如果实体有时间戳，`updatedAt` 不为空
4. ✅ 原有字段（`templateId`, `name`, `version`, `downloadUrl`, `checksum`, `enabled`）保持不变

### 预期响应格式

```json
{
  "success": true,
  "data": [
    {
      "templateId": 1,
      "name": "Template Name",
      "coverUrl": "https://...",
      "version": "1.0.0",
      "downloadUrl": "https://...",
      "checksum": "sha256:...",
      "enabled": true,
      "updatedAt": "2026-02-04T12:00:00Z"
    }
  ],
  "message": null
}
```

## 代码变更清单

### 修改的文件

1. `src/main/java/com/mg/platform/service/DeviceService.java`
   - `TemplateInfo` 类：新增 `updatedAt` 字段和 getter
   - `TemplateInfo` 构造函数：新增 `updatedAt` 参数
   - `getActivityTemplates()` 方法：实现 `updatedAt` 取值逻辑和格式转换

### 新增的文件

1. `test_templates_updated_at.ps1` - PowerShell 测试脚本
2. `TEMPLATES_UPDATED_AT_CHANGES.md` - 本文档

## 注意事项

1. **时区处理**: `updatedAt` 统一使用 UTC 时区，避免设备端时区问题
2. **空值处理**: 如果所有实体都没有时间戳，`updatedAt` 为 `null`，设备端需要处理这种情况
3. **性能**: 时间戳转换开销很小，不影响接口性能

## 后续建议

- 设备端可以使用 `updatedAt` 实现：
  - 离线缓存时记录缓存时间
  - 增量更新：只下载 `updatedAt` 大于本地缓存时间的模板
  - 变更提示：显示模板最后更新时间
