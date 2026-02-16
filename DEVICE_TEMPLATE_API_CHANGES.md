# 设备端模板 API 协议变更

## 变更概述

将设备端模板输出协议从 numeric `templateId`/`version` 改为对外稳定的 `templateCode` + `versionSemver`，同时保留内部 numeric id 不影响 DB 关联。

## 修改内容

### 1. TemplateInfo DTO 字段变更

**新增字段：**
- `templateCode` (String): 来自 `Template.code`，对外稳定的模板标识符
- `versionSemver` (String): 来自 `TemplateVersion.version`，确保是 semver 格式（如 "0.1.1"）
- `checksumSha256` (String): 来自 `TemplateVersion.checksum`，字段名统一为 `checksumSha256`

**保留字段（标记为 deprecated）：**
- `templateId` (Long): 内部 numeric ID，保留用于向后兼容，后续将移除
- `version` (String): 原版本字段，保留用于向后兼容，后续将移除

**其他字段：**
- `name` (String): 模板名称
- `coverUrl` (String): 封面图 URL
- `downloadUrl` (String): 模板包下载 URL（来自 `TemplateVersion.packageUrl`）
- `enabled` (Boolean): 是否启用（来自 `ActivityTemplate.isEnabled`）
- `updatedAt` (String): 更新时间，ISO8601 格式（UTC）

### 2. getActivityTemplates() 方法修改

- 验证 `Template.code` 非空，否则抛出异常：`Template.code is required for device output`
- 使用 `t.getCode()` 作为 `templateCode`
- 使用 `tv.getVersion()` 作为 `versionSemver`
- 使用 `tv.getChecksum()` 作为 `checksumSha256`
- 保持鉴权逻辑不变（deviceToken 校验）

### 3. 修改文件

- `src/main/java/com/mg/platform/service/DeviceService.java`
  - 修改 `TemplateInfo` 内部类：新增字段、重命名字段、标记 deprecated 字段
  - 修改 `getActivityTemplates()` 方法：添加 code 验证、更新字段映射

## JSON 输出示例

### 完整响应示例

```json
{
  "ok": true,
  "data": [
    {
      "templateCode": "tpl_001",
      "versionSemver": "0.1.0",
      "checksumSha256": "a1b2c3d4e5f6...",
      "downloadUrl": "https://example.com/packages/tpl_001-0.1.0.zip",
      "enabled": true,
      "updatedAt": "2026-02-04T12:00:00Z",
      "name": "模板名称",
      "coverUrl": "https://example.com/covers/tpl_001.png",
      "templateId": 1,
      "version": "0.1.0"
    },
    {
      "templateCode": "tpl_002",
      "versionSemver": "1.0.0",
      "checksumSha256": "f6e5d4c3b2a1...",
      "downloadUrl": "https://example.com/packages/tpl_002-1.0.0.zip",
      "enabled": true,
      "updatedAt": "2026-02-04T11:30:00Z",
      "name": "另一个模板",
      "coverUrl": "https://example.com/covers/tpl_002.png",
      "templateId": 2,
      "version": "1.0.0"
    }
  ]
}
```

### 字段说明

| 字段名 | 类型 | 说明 | 状态 |
|--------|------|------|------|
| `templateCode` | String | 模板代码（来自 Template.code），对外稳定标识符 | **新字段** |
| `versionSemver` | String | 版本号（semver 格式，如 "0.1.0"） | **新字段** |
| `checksumSha256` | String | SHA256 校验和 | **重命名**（原 `checksum`） |
| `downloadUrl` | String | 模板包下载 URL | 保留 |
| `enabled` | Boolean | 是否启用 | 保留 |
| `updatedAt` | String | 更新时间（ISO8601 UTC） | 保留 |
| `name` | String | 模板名称 | 保留 |
| `coverUrl` | String | 封面图 URL | 保留 |
| `templateId` | Long | 内部 numeric ID | **Deprecated**（兼容字段） |
| `version` | String | 原版本字段 | **Deprecated**（兼容字段） |

### 兼容性说明

- **新字段**：`templateCode`、`versionSemver`、`checksumSha256` 是主要使用的字段
- **Deprecated 字段**：`templateId` 和 `version` 保留用于向后兼容，旧版 kiosk/UI 仍可使用，但建议迁移到新字段
- **字段名规范**：所有字段使用 camelCase 命名

## 测试建议

### 单元/集成测试要点

1. **字段存在性验证**：
   - 验证 `templateCode` 存在且非空
   - 验证 `versionSemver` 存在且非空
   - 验证 `downloadUrl` 存在且非空
   - 验证 `checksumSha256` 存在且非空

2. **数据来源验证**：
   - `templateCode` 来自 `Template.code`
   - `versionSemver` 来自 `TemplateVersion.version`
   - `checksumSha256` 来自 `TemplateVersion.checksum`
   - `downloadUrl` 来自 `TemplateVersion.packageUrl`
   - `enabled` 来自 `ActivityTemplate.isEnabled`

3. **异常场景**：
   - 当 `Template.code` 为空时，应抛出 `RuntimeException`，消息包含 "Template.code is required for device output"

4. **向后兼容性**：
   - 验证 `templateId` 和 `version` 字段仍存在于响应中（deprecated 但保留）

## 迁移建议

### 设备端（MVP/Kiosk）

1. **优先使用新字段**：
   - 使用 `templateCode` 替代 `templateId`
   - 使用 `versionSemver` 替代 `version`
   - 使用 `checksumSha256` 替代 `checksum`

2. **模板安装请求**：
   ```json
   {
     "templateCode": "tpl_001",
     "version": "0.1.0",
     "downloadUrl": "https://...",
     "checksumSha256": "a1b2c3..."
   }
   ```

3. **逐步移除对 deprecated 字段的依赖**

### 平台端

1. **确保所有 Template.code 非空**：
   - 数据库约束：`templates.code` 字段已设置为 `NOT NULL UNIQUE`
   - 业务逻辑：创建/更新模板时必须提供 code

2. **版本号格式**：
   - 确保 `TemplateVersion.version` 遵循 semver 格式（如 "0.1.0", "1.0.0"）

## 后续计划

- **Phase 1（当前）**：添加新字段，保留 deprecated 字段
- **Phase 2（未来）**：在设备端完全迁移到新字段后，移除 deprecated 字段
- **Phase 3（未来）**：清理相关代码和文档
