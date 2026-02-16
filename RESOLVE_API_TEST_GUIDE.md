# Resolve API 测试指南

## 前置条件

1. MySQL 8.0 已启动
2. 数据库 `ai_photo_booth` 已创建
3. Flyway 迁移已执行（包括 V6）
4. Spring Boot 应用已启动（端口 8089）

## 步骤 1: 准备测试数据

### 1.1 插入 Model Providers

```sql
-- 插入多个测试 provider
INSERT INTO model_providers (code, name, status) VALUES
  ('aliyun', '阿里云', 'ACTIVE'),
  ('volc', '火山引擎', 'ACTIVE'),
  ('local_sd', '本地 Stable Diffusion', 'ACTIVE'),
  ('rembg', 'Remove Background', 'ACTIVE');
```

### 1.2 插入 Provider Capabilities

```sql
-- 获取 provider IDs（根据实际插入后的 ID 调整）
-- 假设：aliyun=2, volc=3, local_sd=4, rembg=5

-- 为 segmentation capability 创建多个 provider
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) VALUES
  (2, 'segmentation', 'https://api.aliyun.com/v1/segmentation', 'ACTIVE', 100, 8000, '{"model":"sam","quality":"high"}'),
  (3, 'segmentation', 'https://api.volcengine.com/v1/segmentation', 'ACTIVE', 200, 10000, '{"model":"u2net","quality":"medium"}'),
  (4, 'segmentation', 'http://localhost:7860/api/segmentation', 'ACTIVE', 300, 15000, '{"model":"local_sam","quality":"high"}'),
  (5, 'segmentation', 'https://api.rembg.com/v1/remove-bg', 'ACTIVE', 400, 5000, '{"model":"u2net"}');

-- 为 background_generation capability 创建
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) VALUES
  (2, 'background_generation', 'https://api.aliyun.com/v1/bg-gen', 'ACTIVE', 100, 12000, '{"style":"realistic"}'),
  (3, 'background_generation', 'https://api.volcengine.com/v1/bg-gen', 'ACTIVE', 200, 10000, '{"style":"artistic"}');
```

### 1.3 插入 GLOBAL Routing Policy（可选）

```sql
-- 为 segmentation 创建 GLOBAL routing policy
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status) VALUES
  ('GLOBAL', NULL, 'segmentation', '["aliyun","volc","local_sd"]', 'ACTIVE');
```

## 步骤 2: 测试场景

### 场景 1: 基础测试（无 prefer，无 routing policy）

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation"
  }'
```

**预期结果**:
- 返回 priority 最低的 provider（aliyun, priority=100）
- `providerCode`: "aliyun"
- `endpoint`: "https://api.aliyun.com/v1/segmentation"
- `timeoutMs`: 8000
- `params`: `{"model":"sam","quality":"high"}`

### 场景 2: 带 prefer 参数

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "prefer": ["volc", "aliyun"]
  }'
```

**预期结果**:
- 优先返回 volc（prefer 第一个）
- `providerCode`: "volc"
- `endpoint`: "https://api.volcengine.com/v1/segmentation"
- `timeoutMs`: 10000
- `params`: `{"model":"u2net","quality":"medium"}`

### 场景 3: 使用 GLOBAL routing policy（无请求 prefer）

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation"
  }'
```

**预期结果**（如果已插入 routing policy）:
- 使用 GLOBAL policy 的 prefer: ["aliyun","volc","local_sd"]
- 返回 aliyun（prefer 第一个）
- `providerCode`: "aliyun"

### 场景 4: 请求 prefer 优先于 GLOBAL policy

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "prefer": ["rembg", "local_sd"]
  }'
```

**预期结果**:
- 请求 prefer 优先，返回 rembg
- `providerCode`: "rembg"
- `endpoint`: "https://api.rembg.com/v1/remove-bg"

### 场景 5: 带 constraints 和 hintParams

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "prefer": ["aliyun"],
    "constraints": {
      "timeoutMs": 15000
    },
    "hintParams": {
      "quality": "ultra",
      "format": "png"
    }
  }'
```

**预期结果**:
- `providerCode`: "aliyun"
- `timeoutMs`: 15000（覆盖默认 8000）
- `params`: `{"model":"sam","quality":"ultra","format":"png"}`（hintParams 覆盖 quality，新增 format）

### 场景 6: 测试 background_generation capability

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "background_generation",
    "prefer": ["volc"]
  }'
```

**预期结果**:
- `providerCode`: "volc"
- `endpoint`: "https://api.volcengine.com/v1/bg-gen"
- `params`: `{"style":"artistic"}`

### 场景 7: 错误场景 - capability 不存在

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "non_existent_capability"
  }'
```

**预期结果**:
```json
{
  "success": false,
  "message": "No active provider found for capability: non_existent_capability",
  "data": null
}
```

## 步骤 3: 完整测试脚本

### Windows PowerShell 测试脚本

创建 `test_resolve_api.ps1`:

```powershell
# 测试 Resolve API

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "=== 测试 1: 基础测试 ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
} | ConvertTo-Json
Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1 | ConvertTo-Json -Depth 10

Write-Host "`n=== 测试 2: 带 prefer ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc", "aliyun")
} | ConvertTo-Json
Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2 | ConvertTo-Json -Depth 10

Write-Host "`n=== 测试 3: 带 constraints 和 hintParams ===" -ForegroundColor Green
$body3 = @{
    capability = "segmentation"
    prefer = @("aliyun")
    constraints = @{
        timeoutMs = 15000
    }
    hintParams = @{
        quality = "ultra"
        format = "png"
    }
} | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body3 | ConvertTo-Json -Depth 10

Write-Host "`n=== 测试 4: 错误场景 ===" -ForegroundColor Yellow
$body4 = @{
    capability = "non_existent"
} | ConvertTo-Json
try {
    Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body4 | ConvertTo-Json
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
}
```

### Linux/Mac Bash 测试脚本

创建 `test_resolve_api.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8089/api/v1/ai/resolve"

echo "=== 测试 1: 基础测试 ==="
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation"}' | jq .

echo -e "\n=== 测试 2: 带 prefer ==="
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation","prefer":["volc","aliyun"]}' | jq .

echo -e "\n=== 测试 3: 带 constraints 和 hintParams ==="
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "capability":"segmentation",
    "prefer":["aliyun"],
    "constraints":{"timeoutMs":15000},
    "hintParams":{"quality":"ultra","format":"png"}
  }' | jq .

echo -e "\n=== 测试 4: 错误场景 ==="
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"capability":"non_existent"}' | jq .
```

## 步骤 4: 验证数据库查询

### 检查数据是否正确插入

```sql
-- 查看所有 providers
SELECT * FROM model_providers;

-- 查看所有 capabilities
SELECT 
  pc.id,
  mp.code as provider_code,
  pc.capability,
  pc.endpoint,
  pc.priority,
  pc.default_timeout_ms,
  pc.default_params_json
FROM provider_capabilities pc
JOIN model_providers mp ON pc.provider_id = mp.id
WHERE pc.status = 'ACTIVE'
ORDER BY pc.capability, pc.priority;

-- 查看 routing policies
SELECT * FROM capability_routing_policies WHERE status = 'ACTIVE';
```

## 步骤 5: 预期响应格式

### 成功响应示例

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "mode": "direct",
    "capability": "segmentation",
    "direct": {
      "providerCode": "aliyun",
      "endpoint": "https://api.aliyun.com/v1/segmentation",
      "auth": {
        "type": "api_key",
        "apiKey": ""
      },
      "timeoutMs": 8000,
      "params": {
        "model": "sam",
        "quality": "high"
      }
    }
  }
}
```

### 错误响应示例

```json
{
  "success": false,
  "message": "No active provider found for capability: non_existent",
  "data": null
}
```

## 快速测试命令（单行）

```bash
# Windows PowerShell
curl -X POST http://localhost:8089/api/v1/ai/resolve -H "Content-Type: application/json" -d '{\"capability\":\"segmentation\"}'

# Linux/Mac
curl -X POST http://localhost:8089/api/v1/ai/resolve -H "Content-Type: application/json" -d '{"capability":"segmentation"}'
```

## 注意事项

1. **Provider ID**: 插入数据时，需要先查询 `model_providers` 表获取实际的 ID，然后用于 `provider_capabilities.provider_id`
2. **JSON 字段**: `default_params_json` 和 `prefer_providers_json` 必须是有效的 JSON 字符串
3. **NULL 值**: GLOBAL routing policy 的 `merchant_id` 必须为 NULL
4. **优先级**: priority 值越小，优先级越高

## 故障排查

1. **403 Forbidden**: 检查 SecurityConfig 是否允许 `/api/v1/ai/**`
2. **500 Internal Server Error**: 查看应用日志，检查数据库连接和 SQL 错误
3. **空结果**: 确认测试数据已正确插入，检查 `status='ACTIVE'`
4. **JSON 解析错误**: 检查 `default_params_json` 和 `prefer_providers_json` 格式是否正确
