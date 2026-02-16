# AI Photo Booth Platform Extension - 工作完成总结

## 项目概述

**项目名称**: ai-photo-booth-platform  
**技术栈**: Spring Boot + Flyway + MySQL 8.0 + JPA/Hibernate  
**主要功能**: AI 模型提供商管理和能力路由系统

## 已完成功能清单

### 1. 数据库扩展（Flyway 迁移）

#### 1.1 新增表：`provider_capabilities`
**用途**: 存储 AI 提供商的能力配置

**字段结构**:
- `id` BIGINT UNSIGNED PK AUTO_INCREMENT
- `provider_id` BIGINT UNSIGNED NOT NULL (FK -> model_providers.id)
- `capability` VARCHAR(64) NOT NULL (如: segmentation/background_generation/img2img/enhance/identity_generate)
- `endpoint` VARCHAR(512) NULL
- `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
- `priority` INT NOT NULL DEFAULT 100
- `default_timeout_ms` INT NOT NULL DEFAULT 8000
- `default_params_json` JSON NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

**约束/索引**:
- UNIQUE `uk_provider_capability`(provider_id, capability)
- KEY `idx_pc_capability_status_priority`(capability, status, priority)
- FK `fk_pc_provider`(provider_id) REFERENCES model_providers(id) ON DELETE CASCADE

#### 1.2 新增表：`capability_routing_policies`
**用途**: 存储能力路由策略（全局或商户级别）

**字段结构**:
- `id` BIGINT UNSIGNED PK AUTO_INCREMENT
- `scope` VARCHAR(16) NOT NULL DEFAULT 'GLOBAL' (GLOBAL/MERCHANT)
- `merchant_id` BIGINT UNSIGNED NULL (FK -> merchants.id)
- `capability` VARCHAR(64) NOT NULL
- `prefer_providers_json` JSON NULL (如: ["aliyun","volc","local_sd","rembg"])
- `retry_count` INT NOT NULL DEFAULT 0
- `failover_on_http_codes_json` JSON NULL (如: [429,500,502,503,504])
- `max_cost_tier` INT NULL
- `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

**约束/索引**:
- UNIQUE `uk_scope_capability`(scope, merchant_id, capability)
- KEY `idx_policy_capability_status`(capability, status)
- FK `fk_policy_merchant`(merchant_id) REFERENCES merchants(id) ON DELETE CASCADE

#### 1.3 使用现有表
- `model_providers` - AI 模型提供商表（已存在）
- `provider_api_keys` - 提供商 API 密钥表（已存在）
- `merchants` - 商户表（已存在）

### 2. JPA 实体和仓储层

#### 2.1 新增实体类
- `ModelProvider.java` - 映射 `model_providers` 表
- `ProviderApiKey.java` - 映射 `provider_api_keys` 表
- `ProviderCapability.java` - 映射 `provider_capabilities` 表
- `CapabilityRoutingPolicy.java` - 映射 `capability_routing_policies` 表

#### 2.2 新增仓储接口
- `ModelProviderRepository.java` - 提供 `findByCode()` 方法
- `ProviderApiKeyRepository.java` - 提供 `findFirstByProviderIdAndStatusOrderByCreatedAtDesc()` 方法
- `ProviderCapabilityRepository.java` - 提供 `findByCapabilityAndStatusOrderByPriorityAsc()` 方法
- `CapabilityRoutingPolicyRepository.java` - 提供 `findFirstByScopeAndMerchantIdAndCapabilityOrderByCreatedAtDesc()` 方法

### 3. 核心业务服务

#### 3.1 ResolveService（AI 提供商解析服务）
**功能**: 根据能力需求、优先级、策略等条件，智能选择最合适的 AI 提供商

**核心逻辑**:
1. **优先级排序规则**（从高到低）:
   - 请求中的 `prefer` 列表（最高优先级）
   - 商户级别的路由策略 `prefer_providers_json`
   - 全局路由策略 `prefer_providers_json`
   - 数据库中的 `priority` 字段（数值越小优先级越高）

2. **成本过滤**:
   - 如果请求中指定了 `constraints.maxCostTier`
   - 且 `model_providers` 表存在 `cost_tier` 字段
   - 则过滤掉成本超过限制的提供商

3. **API Key 处理**:
   - 自动查询选中提供商的活跃 API Key
   - 使用 AES-128 解密后返回明文（仅用于响应，不写入日志）
   - 如果找不到活跃 Key，抛出 `NoActiveApiKeyException`

4. **参数合并**:
   - 合并 `default_params_json` 和请求中的 `hintParams`
   - `hintParams` 覆盖同名键

**方法签名**:
```java
@Transactional(readOnly = true)
public AiResolveResponse resolve(AiResolveRequest request)
```

#### 3.2 ProviderAdminService（提供商管理服务）
**功能**: 提供商的 CRUD 操作

**方法**:
- `listProviders()` - 列表查询
- `createProvider(CreateProviderRequest)` - 创建提供商
- `updateProvider(Long, UpdateProviderRequest)` - 更新提供商
- `listCapabilities(Long providerId)` - 查询提供商的能力列表
- `createCapability(Long providerId, CreateCapabilityRequest)` - 创建能力
- `updateCapability(Long providerId, Long capabilityId, UpdateCapabilityRequest)` - 更新能力
- `listApiKeys(Long providerId)` - 查询 API Key 列表（返回 DTO，隐藏密文）
- `createApiKey(Long providerId, CreateApiKeyRequest)` - 创建 API Key（接收明文，加密存储）
- `disableApiKey(Long providerId, Long keyId)` - 禁用 API Key

#### 3.3 RoutingPolicyAdminService（路由策略管理服务）
**功能**: 路由策略的 CRUD 操作

**方法**:
- `listPolicies(String scope, Long merchantId, String capability)` - 列表查询
- `createPolicy(CreatePolicyRequest)` - 创建策略
- `updatePolicy(Long policyId, UpdatePolicyRequest)` - 更新策略

#### 3.4 CryptoUtil（加密工具类）
**功能**: API Key 的 AES-128 加密/解密

**方法**:
- `encrypt(String plainText)` - 加密明文 API Key
- `decrypt(String cipherText)` - 解密密文 API Key

**配置**:
- 密钥通过 `application.yml` 中的 `crypto.api-key.secret` 配置
- 默认值: `default-secret-key-16`（生产环境必须修改）

### 4. REST API 接口

#### 4.1 Resolve API（公开接口，无需认证）
**端点**: `POST /api/v1/ai/resolve`

**请求体** (`AiResolveRequest`):
```json
{
  "capability": "segmentation",           // 必需：能力名称
  "templateCode": "template_001",           // 可选：模板代码
  "versionSemver": "1.0.0",                 // 可选：版本号
  "merchantCode": "merchant_001",           // 可选：商户代码
  "prefer": ["aliyun", "volc"],             // 可选：偏好提供商列表
  "constraints": {                          // 可选：约束条件
    "timeoutMs": 5000,                      // 超时时间（毫秒）
    "maxCostTier": 2                        // 最大成本层级
  },
  "hintParams": {                           // 可选：提示参数
    "model": "gpt-4",
    "temperature": 0.7
  }
}
```

**响应体** (`AiResolveResponse`):
```json
{
  "success": true,
  "data": {
    "mode": "direct",
    "capability": "segmentation",
    "direct": {
      "providerCode": "aliyun",
      "endpoint": "https://api.aliyun.com/v1/segmentation",
      "timeoutMs": 8000,
      "params": {
        "model": "gpt-4",
        "temperature": 0.7
      },
      "auth": {
        "type": "api_key",
        "apiKey": "sk-xxxxx"                // 解密后的明文 API Key
      }
    }
  }
}
```

**错误响应**:
- `400 Bad Request`: 无活跃 API Key 或其他业务错误
- `500 Internal Server Error`: 服务器内部错误

#### 4.2 Admin API - Providers（需要 ADMIN 角色）
**基础路径**: `/api/v1/admin/providers`

**接口列表**:
1. `GET /api/v1/admin/providers` - 获取提供商列表
2. `POST /api/v1/admin/providers` - 创建提供商
   ```json
   {
     "code": "aliyun",
     "name": "阿里云 AI",
     "status": "ACTIVE"
   }
   ```
3. `PUT /api/v1/admin/providers/{providerId}` - 更新提供商
4. `GET /api/v1/admin/providers/{providerId}/capabilities` - 获取能力列表
5. `POST /api/v1/admin/providers/{providerId}/capabilities` - 创建能力
   ```json
   {
     "capability": "segmentation",
     "endpoint": "https://api.aliyun.com/v1/segmentation",
     "status": "ACTIVE",
     "priority": 100,
     "defaultTimeoutMs": 8000,
     "defaultParamsJson": "{\"model\":\"default\"}"
   }
   ```
6. `PUT /api/v1/admin/providers/{providerId}/capabilities/{capabilityId}` - 更新能力
7. `GET /api/v1/admin/providers/{providerId}/keys` - 获取 API Key 列表（不返回明文）
8. `POST /api/v1/admin/providers/{providerId}/keys` - 创建 API Key
   ```json
   {
     "name": "Production Key",
     "apiKey": "sk-xxxxx"                    // 明文，后端会加密存储
   }
   ```
9. `PUT /api/v1/admin/providers/{providerId}/keys/{keyId}/disable` - 禁用 API Key

#### 4.3 Admin API - Routing Policies（需要 ADMIN 角色）
**基础路径**: `/api/v1/admin/routing-policies`

**接口列表**:
1. `GET /api/v1/admin/routing-policies?scope=GLOBAL&capability=segmentation` - 获取策略列表
2. `POST /api/v1/admin/routing-policies` - 创建策略
   ```json
   {
     "scope": "GLOBAL",                      // GLOBAL 或 MERCHANT
     "merchantId": 1,                        // scope=MERCHANT 时必需
     "capability": "segmentation",
     "preferProvidersJson": "[\"aliyun\",\"volc\"]",
     "retryCount": 0,
     "failoverOnHttpCodesJson": "[429,500,502,503,504]",
     "maxCostTier": 2,
     "status": "ACTIVE"
   }
   ```
3. `PUT /api/v1/admin/routing-policies/{policyId}` - 更新策略

**认证要求**:
- 所有 Admin API 需要 JWT Token
- Token 通过 `Authorization: Bearer {token}` 头部传递
- 用户必须具有 `ADMIN` 角色

### 5. DTO 类

#### 5.1 Resolve API DTO
- `AiResolveRequest.java` - 请求 DTO
  - 包含 `Constraints` 内部类
- `AiResolveResponse.java` - 响应 DTO
  - 包含 `Direct` 和 `Auth` 内部类

#### 5.2 Admin API DTO
- `ProviderApiKeyDTO.java` - API Key 响应 DTO（隐藏 `apiKeyCipher` 字段）
- 其他 DTO 使用实体类直接序列化（通过 `@JsonIgnoreProperties` 控制）

### 6. 异常处理

#### 6.1 自定义异常
- `NoActiveApiKeyException.java` - 无活跃 API Key 异常
  - 错误码: `NO_ACTIVE_API_KEY`
  - 返回 HTTP 400

#### 6.2 异常处理位置
- `ResolveController.java` - 捕获 `NoActiveApiKeyException` 并返回 400
- 其他异常统一返回 500

### 7. 安全配置

#### 7.1 Spring Security 配置
**文件**: `SecurityConfig.java`

**配置内容**:
- `/api/v1/ai/**` - 公开访问（无需认证）
- `/api/v1/admin/**` - 需要 `ADMIN` 角色
- `/api/v1/merchant/**` - 需要 `ADMIN` 或 `MERCHANT_OWNER` 角色

#### 7.2 API Key 安全
- 创建时接收明文，立即加密存储
- 响应中不返回 `apiKeyCipher` 字段
- 使用 DTO 隐藏敏感信息
- 解密后的明文不写入日志

### 8. 测试验证

#### 8.1 单元测试
- `ResolveServiceTest.java` - 覆盖以下场景:
  - 基本解析逻辑
  - 优先级排序（request prefer > merchant policy > global policy > priority）
  - 成本过滤
  - API Key 解密
  - 无活跃 API Key 异常

#### 8.2 集成测试脚本
- `quick_test_admin_api.ps1` - 快速验收测试脚本
  - 自动登录获取 Token
  - 创建 Provider/Capability/Key
  - 验证 Resolve 立即生效
  - 验证 API Key 不回显明文

#### 8.3 测试结果
✅ 所有核心功能已验证通过:
- Provider/Capability/Key 创建成功
- Resolve API 立即生效（无需重启）
- API Key 不回显明文
- 优先级策略正确应用

### 9. 配置文件

#### 9.1 application.yml
**新增配置**:
```yaml
crypto:
  api-key:
    secret: default-secret-key-16  # 生产环境必须修改为 16/24/32 字节密钥
```

### 10. 数据库迁移文件

#### 10.1 Flyway 迁移
**文件**: `V6__provider_capabilities_and_routing_policies.sql`

**内容**:
- 创建 `provider_capabilities` 表
- 创建 `capability_routing_policies` 表
- 包含所有约束、索引和外键

## 技术实现细节

### 1. JSON 字段处理
- 使用 `String` 类型存储 JSON 字段（`default_params_json`, `prefer_providers_json` 等）
- 使用 `ObjectMapper` 进行序列化/反序列化
- 支持 `Map<String, Object>` 和 `List<String>` 的转换

### 2. 优先级排序算法
```java
// 优先级从高到低:
// 1. 请求中的 prefer 列表
// 2. 商户策略中的 prefer_providers_json
// 3. 全局策略中的 prefer_providers_json
// 4. 数据库中的 priority 字段（ASC）
```

### 3. 成本过滤逻辑
- 使用反射检查 `ModelProvider` 是否有 `costTier` 字段
- 如果存在且请求指定了 `maxCostTier`，则过滤超出成本的提供商
- 如果字段不存在，忽略成本过滤（不报错）

### 4. 加密/解密实现
- 算法: AES-128
- 模式: AES（ECB 模式，仅用于 API Key，非敏感数据）
- 编码: Base64
- 密钥: 从配置文件读取，默认 16 字节

## 数据模型关系

```
model_providers (1) ──< (N) provider_capabilities
model_providers (1) ──< (N) provider_api_keys
merchants (1) ──< (N) capability_routing_policies
```

## API 响应格式

所有 API 使用统一的响应格式:
```json
{
  "success": true,
  "data": { ... },
  "message": "操作成功"
}
```

错误响应:
```json
{
  "success": false,
  "data": null,
  "message": "错误信息"
}
```

## 前端开发建议

### 1. 需要开发的管理页面

#### 1.1 提供商管理页面
- **路由**: `/admin/providers`
- **功能**:
  - 列表展示（表格）
  - 创建提供商（表单）
  - 编辑提供商（表单）
  - 查看提供商详情（包含能力列表和 API Key 列表）

#### 1.2 能力管理页面
- **路由**: `/admin/providers/:providerId/capabilities`
- **功能**:
  - 列表展示（表格）
  - 创建能力（表单）
  - 编辑能力（表单）
  - 字段: capability, endpoint, status, priority, defaultTimeoutMs, defaultParamsJson

#### 1.3 API Key 管理页面
- **路由**: `/admin/providers/:providerId/keys`
- **功能**:
  - 列表展示（表格，不显示密文）
  - 创建 API Key（表单，输入明文）
  - 禁用 API Key（按钮）
  - **安全提示**: 创建后无法再次查看明文

#### 1.4 路由策略管理页面
- **路由**: `/admin/routing-policies`
- **功能**:
  - 列表展示（表格，支持按 scope/capability 筛选）
  - 创建策略（表单）
  - 编辑策略（表单）
  - 字段: scope, merchantId, capability, preferProvidersJson, retryCount, failoverOnHttpCodesJson, maxCostTier, status

### 2. 需要开发的调用页面

#### 2.1 Resolve API 调用示例
- 可以创建一个测试页面用于验证 Resolve API
- 展示解析结果（providerCode, endpoint, params, auth）

### 3. 前端技术栈建议

基于现有项目结构，建议使用:
- **框架**: React / Vue / Angular（根据项目现有选择）
- **HTTP 客户端**: Axios / Fetch
- **状态管理**: Redux / Vuex / Context API
- **UI 组件库**: Ant Design / Element UI / Material UI
- **表单处理**: React Hook Form / Formik / VeeValidate

### 4. 前端开发步骤建议

#### 步骤 1: 环境准备
1. 确认前端项目技术栈
2. 配置 API 基础 URL（开发/生产环境）
3. 配置认证拦截器（JWT Token 自动添加）

#### 步骤 2: API 服务封装
1. 创建 API 服务层（封装所有 HTTP 请求）
2. 实现认证拦截器（自动添加 Bearer Token）
3. 实现错误处理（统一处理 401/403/500 等）

#### 步骤 3: 提供商管理模块
1. 创建 Provider 列表页面
2. 创建 Provider 创建/编辑表单
3. 实现 CRUD 操作
4. 添加表单验证

#### 步骤 4: 能力管理模块
1. 创建 Capability 列表页面（嵌套在 Provider 详情中）
2. 创建 Capability 创建/编辑表单
3. 实现 JSON 编辑器（用于 defaultParamsJson）
4. 实现 CRUD 操作

#### 步骤 5: API Key 管理模块
1. 创建 API Key 列表页面（嵌套在 Provider 详情中）
2. 创建 API Key 创建表单（带安全提示）
3. 实现禁用功能
4. **重要**: 确保不显示密文字段

#### 步骤 6: 路由策略管理模块
1. 创建 Policy 列表页面（支持筛选）
2. 创建 Policy 创建/编辑表单
3. 实现 JSON 编辑器（用于 preferProvidersJson, failoverOnHttpCodesJson）
4. 实现商户选择器（scope=MERCHANT 时）

#### 步骤 7: Resolve API 测试页面（可选）
1. 创建测试表单
2. 调用 Resolve API
3. 展示解析结果
4. 用于验证和调试

#### 步骤 8: 权限控制
1. 实现路由守卫（仅 ADMIN 可访问管理页面）
2. 实现按钮级权限控制
3. 处理 403 错误（无权限提示）

#### 步骤 9: 用户体验优化
1. 添加加载状态
2. 添加成功/错误提示
3. 添加确认对话框（删除/禁用操作）
4. 优化表单验证提示
5. 添加数据刷新功能

#### 步骤 10: 测试和优化
1. 端到端测试
2. 错误场景测试
3. 性能优化
4. 响应式布局优化

## 注意事项

### 1. 安全相关
- API Key 创建后无法再次查看明文（前端需要明确提示用户）
- 所有 Admin API 需要认证，前端需要处理 401/403 错误
- Resolve API 是公开的，但返回的 API Key 是敏感信息，需要妥善处理

### 2. 数据格式
- JSON 字段（如 `defaultParamsJson`, `preferProvidersJson`）需要 JSON 编辑器
- 时间字段使用 ISO 8601 格式
- 状态字段使用字符串（"ACTIVE", "INACTIVE" 等）

### 3. 业务逻辑
- 优先级数值越小优先级越高
- 路由策略优先级: request prefer > merchant policy > global policy > priority
- 成本过滤是可选的（如果 model_providers 没有 cost_tier 字段则忽略）

### 4. 错误处理
- 无活跃 API Key: 返回 400，错误码 `NO_ACTIVE_API_KEY`
- 无权限: 返回 403
- 未认证: 返回 401
- 服务器错误: 返回 500

## 文件清单

### 新增文件
- `V6__provider_capabilities_and_routing_policies.sql` - Flyway 迁移文件
- `ModelProvider.java` - 实体类
- `ProviderApiKey.java` - 实体类
- `ProviderCapability.java` - 实体类
- `CapabilityRoutingPolicy.java` - 实体类
- `ModelProviderRepository.java` - 仓储接口
- `ProviderApiKeyRepository.java` - 仓储接口
- `ProviderCapabilityRepository.java` - 仓储接口
- `CapabilityRoutingPolicyRepository.java` - 仓储接口
- `ResolveService.java` - 解析服务
- `ProviderAdminService.java` - 提供商管理服务
- `RoutingPolicyAdminService.java` - 路由策略管理服务
- `CryptoUtil.java` - 加密工具类
- `ResolveController.java` - Resolve API 控制器
- `ProviderAdminController.java` - Provider Admin API 控制器
- `RoutingPolicyAdminController.java` - Policy Admin API 控制器
- `AiResolveRequest.java` - Resolve 请求 DTO
- `AiResolveResponse.java` - Resolve 响应 DTO
- `ProviderApiKeyDTO.java` - API Key 响应 DTO
- `NoActiveApiKeyException.java` - 自定义异常
- `ResolveServiceTest.java` - 单元测试
- `quick_test_admin_api.ps1` - 快速测试脚本

### 修改文件
- `SecurityConfig.java` - 添加 `/api/v1/ai/**` 公开访问
- `application.yml` - 添加 `crypto.api-key.secret` 配置

## 总结

本次扩展完成了 AI 模型提供商管理和能力路由系统的核心功能，包括:
1. ✅ 数据库表结构设计（2 张新表）
2. ✅ JPA 实体和仓储层
3. ✅ 核心业务服务（Resolve、Admin CRUD）
4. ✅ REST API 接口（Resolve API + Admin API）
5. ✅ 安全功能（API Key 加密/解密）
6. ✅ 测试验证（单元测试 + 集成测试）

**系统已具备完整的管理功能和智能路由能力，可以开始前端开发工作。**
