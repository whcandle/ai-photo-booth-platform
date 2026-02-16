# Admin CRUD API 文档

## 权限说明

所有 API 都需要 ADMIN 角色权限，由 `SecurityConfig` 统一配置：
- 路径：`/api/v1/admin/**`
- 权限：`hasRole("ADMIN")`

## 新增路由列表

### 1. Providers 管理

#### 1.1 列表 Providers
- **路径**: `GET /api/v1/admin/providers`
- **说明**: 获取所有 providers 列表

#### 1.2 创建 Provider
- **路径**: `POST /api/v1/admin/providers`
- **说明**: 创建新的 provider

#### 1.3 更新 Provider
- **路径**: `PUT /api/v1/admin/providers/{providerId}`
- **说明**: 更新 provider 信息（code 不允许修改）

### 2. Provider Capabilities 管理

#### 2.1 列表 Capabilities
- **路径**: `GET /api/v1/admin/providers/{providerId}/capabilities`
- **说明**: 获取指定 provider 的所有 capabilities

#### 2.2 创建 Capability
- **路径**: `POST /api/v1/admin/providers/{providerId}/capabilities`
- **说明**: 为指定 provider 创建新的 capability

#### 2.3 更新 Capability
- **路径**: `PUT /api/v1/admin/providers/{providerId}/capabilities/{capabilityId}`
- **说明**: 更新 capability 信息（capability 字段不允许修改）

### 3. Provider API Keys 管理

#### 3.1 列表 API Keys
- **路径**: `GET /api/v1/admin/providers/{providerId}/keys`
- **说明**: 获取指定 provider 的所有 ACTIVE 状态的 API Keys
- **响应**: 使用 `ProviderApiKeyDTO`，不包含 `apiKeyCipher` 字段，确保不回显明文

#### 3.2 创建 API Key
- **路径**: `POST /api/v1/admin/providers/{providerId}/keys`
- **说明**: 为指定 provider 创建新的 API Key
- **请求**: 接收明文 `apiKey` 字段
- **处理**: 后端使用 `CryptoUtil.encrypt()` 加密后存储到 `apiKeyCipher`
- **响应**: 使用 `ProviderApiKeyDTO`，不包含 `apiKeyCipher` 字段，确保不回显明文

#### 3.3 禁用 API Key
- **路径**: `PUT /api/v1/admin/providers/{providerId}/keys/{keyId}/disable`
- **说明**: 禁用指定的 API Key（将 status 设置为 INACTIVE）
- **响应**: 使用 `ProviderApiKeyDTO`，不包含 `apiKeyCipher` 字段

### 4. Routing Policies 管理

#### 4.1 列表 Policies
- **路径**: `GET /api/v1/admin/routing-policies`
- **说明**: 获取所有 routing policies 列表

#### 4.2 创建 Policy
- **路径**: `POST /api/v1/admin/routing-policies`
- **说明**: 创建新的 routing policy

#### 4.3 更新 Policy
- **路径**: `PUT /api/v1/admin/routing-policies/{policyId}`
- **说明**: 更新 policy 信息（scope, merchant, capability 不允许修改）

## 示例请求体

### 1. 创建 Provider

```json
POST /api/v1/admin/providers
{
  "code": "aliyun",
  "name": "阿里云",
  "status": "ACTIVE"
}
```

### 2. 更新 Provider

```json
PUT /api/v1/admin/providers/1
{
  "name": "阿里云（更新）",
  "status": "ACTIVE"
}
```

### 3. 创建 Capability

```json
POST /api/v1/admin/providers/1/capabilities
{
  "capability": "segmentation",
  "endpoint": "https://api.aliyun.com/v1/segmentation",
  "status": "ACTIVE",
  "priority": 100,
  "defaultTimeoutMs": 8000,
  "defaultParamsJson": "{\"model\":\"sam\",\"quality\":\"high\"}"
}
```

### 4. 更新 Capability

```json
PUT /api/v1/admin/providers/1/capabilities/1
{
  "endpoint": "https://api.aliyun.com/v2/segmentation",
  "status": "ACTIVE",
  "priority": 50,
  "defaultTimeoutMs": 10000,
  "defaultParamsJson": "{\"model\":\"sam-v2\",\"quality\":\"high\"}"
}
```

### 5. 创建 API Key

**请求**:
```json
POST /api/v1/admin/providers/1/keys
{
  "name": "Aliyun Main API Key",
  "apiKey": "sk-aliyun-secret-key-12345"
}
```

**响应**（不回显明文）:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "providerId": 1,
    "providerCode": "aliyun",
    "name": "Aliyun Main API Key",
    "status": "ACTIVE",
    "createdAt": "2026-02-15T08:00:00",
    "updatedAt": "2026-02-15T08:00:00"
  }
}
```

**注意**：
- `apiKey` 字段接收明文
- 后端使用 `CryptoUtil.encrypt()` 加密后存储到 `apiKeyCipher`
- 响应中使用 `ProviderApiKeyDTO`，**不包含 `apiKeyCipher` 字段**，确保不回显明文

### 6. 禁用 API Key

```json
PUT /api/v1/admin/providers/1/keys/1/disable
```

（无需请求体，直接将 status 设置为 INACTIVE）

### 7. 创建 Routing Policy (GLOBAL)

```json
POST /api/v1/admin/routing-policies
{
  "scope": "GLOBAL",
  "capability": "segmentation",
  "preferProvidersJson": "[\"aliyun\", \"volc\"]",
  "retryCount": 0,
  "failoverOnHttpCodesJson": "[429, 500, 502, 503, 504]",
  "maxCostTier": 2,
  "status": "ACTIVE"
}
```

### 8. 创建 Routing Policy (MERCHANT)

```json
POST /api/v1/admin/routing-policies
{
  "scope": "MERCHANT",
  "merchantCode": "TEST001",
  "capability": "segmentation",
  "preferProvidersJson": "[\"volc\"]",
  "retryCount": 1,
  "failoverOnHttpCodesJson": "[429, 500]",
  "maxCostTier": 1,
  "status": "ACTIVE"
}
```

### 9. 更新 Routing Policy

```json
PUT /api/v1/admin/routing-policies/1
{
  "preferProvidersJson": "[\"volc\", \"aliyun\"]",
  "retryCount": 2,
  "failoverOnHttpCodesJson": "[429, 500, 502]",
  "maxCostTier": 3,
  "status": "ACTIVE"
}
```

## 响应格式

所有 API 使用统一的 `ApiResponse` 格式：

### 成功响应

```json
{
  "success": true,
  "message": "Success",
  "data": {
    // 具体数据
  }
}
```

### 错误响应

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

## 注意事项

1. **API Key 安全**：
   - 创建时接收明文，后端自动加密存储
   - 列表和详情接口不回显明文，只返回加密后的 `apiKeyCipher`
   - 禁用操作将 status 设置为 INACTIVE，不会删除记录

2. **唯一性约束**：
   - Provider code 必须唯一
   - Provider 的 capability 必须唯一（同一 provider 不能有重复的 capability）
   - Routing policy 的 (scope, merchant_id, capability) 必须唯一

3. **不允许修改的字段**：
   - Provider: code
   - Capability: capability 字段
   - Routing Policy: scope, merchant, capability

4. **默认值**：
   - Provider status: "ACTIVE"
   - Capability status: "ACTIVE", priority: 100, defaultTimeoutMs: 8000
   - Routing Policy scope: "GLOBAL", status: "ACTIVE", retryCount: 0

## 测试示例

### 创建完整的 Provider 配置

```bash
# 1. 创建 Provider
curl -X POST http://localhost:8089/api/v1/admin/providers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "code": "aliyun",
    "name": "阿里云",
    "status": "ACTIVE"
  }'

# 2. 创建 Capability
curl -X POST http://localhost:8089/api/v1/admin/providers/1/capabilities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "capability": "segmentation",
    "endpoint": "https://api.aliyun.com/v1/segmentation",
    "status": "ACTIVE",
    "priority": 100,
    "defaultTimeoutMs": 8000
  }'

# 3. 创建 API Key
curl -X POST http://localhost:8089/api/v1/admin/providers/1/keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "name": "Aliyun Main Key",
    "apiKey": "sk-aliyun-secret-12345"
  }'
```
