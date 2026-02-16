# Admin CRUD API 测试指南

## 测试目标

验证以下功能：
1. ✅ 能够通过 API 创建 provider/capability/key
2. ✅ Resolve API 能够立即生效（无需重启应用）
3. ✅ 更新功能正常工作
4. ✅ API Key 不回显明文

## 前置条件

1. **应用已启动**
   ```bash
   mvn spring-boot:run
   ```

2. **数据库已准备**
   - MySQL 8.0 运行中
   - 数据库 `ai_photo_booth` 已创建
   - Flyway 迁移已执行

3. **认证信息**（如果需要）
   - 如果启用了认证，需要获取 admin token
   - 或者确保测试环境允许匿名访问 admin API

## 自动化测试（推荐）

### 方法 1: 使用 PowerShell 脚本

```powershell
cd D:\workspace\ai-photo-booth-platform
.\test_admin_crud_api.ps1
```

**脚本功能**：
- 自动创建 provider/capability/key
- 验证 resolve 立即生效
- 测试列表和更新功能
- 验证 API Key 不回显明文
- 生成测试报告

### 方法 2: 使用 Postman Collection

导入以下 Postman Collection（JSON）：

```json
{
  "info": {
    "name": "Admin CRUD API Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Create Provider",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"code\": \"test_provider\",\n  \"name\": \"Test Provider\",\n  \"status\": \"ACTIVE\"\n}"
        },
        "url": {
          "raw": "http://localhost:8089/api/v1/admin/providers",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8089",
          "path": ["api", "v1", "admin", "providers"]
        }
      }
    },
    {
      "name": "2. Create Capability",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"capability\": \"segmentation\",\n  \"endpoint\": \"https://api.test.com/v1/segmentation\",\n  \"status\": \"ACTIVE\",\n  \"priority\": 100,\n  \"defaultTimeoutMs\": 8000\n}"
        },
        "url": {
          "raw": "http://localhost:8089/api/v1/admin/providers/{{providerId}}/capabilities",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8089",
          "path": ["api", "v1", "admin", "providers", "{{providerId}}", "capabilities"]
        }
      }
    },
    {
      "name": "3. Create API Key",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Test API Key\",\n  \"apiKey\": \"sk-test-key-12345\"\n}"
        },
        "url": {
          "raw": "http://localhost:8089/api/v1/admin/providers/{{providerId}}/keys",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8089",
          "path": ["api", "v1", "admin", "providers", "{{providerId}}", "keys"]
        }
      }
    },
    {
      "name": "4. Test Resolve (Immediate)",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"capability\": \"segmentation\",\n  \"prefer\": [\"test_provider\"]\n}"
        },
        "url": {
          "raw": "http://localhost:8089/api/v1/ai/resolve",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8089",
          "path": ["api", "v1", "ai", "resolve"]
        }
      }
    }
  ]
}
```

## 手动测试步骤

### 步骤 1: 创建 Provider

```bash
curl -X POST http://localhost:8089/api/v1/admin/providers \
  -H "Content-Type: application/json" \
  -d '{
    "code": "test_provider",
    "name": "Test Provider",
    "status": "ACTIVE"
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "code": "test_provider",
    "name": "Test Provider",
    "status": "ACTIVE"
  }
}
```

**记录**: `providerId = 1`

### 步骤 2: 创建 Capability

```bash
curl -X POST http://localhost:8089/api/v1/admin/providers/1/capabilities \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "endpoint": "https://api.test.com/v1/segmentation",
    "status": "ACTIVE",
    "priority": 100,
    "defaultTimeoutMs": 8000
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "capability": "segmentation",
    "endpoint": "https://api.test.com/v1/segmentation",
    "status": "ACTIVE",
    "priority": 100,
    "defaultTimeoutMs": 8000
  }
}
```

### 步骤 3: 创建 API Key

```bash
curl -X POST http://localhost:8089/api/v1/admin/providers/1/keys \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test API Key",
    "apiKey": "sk-test-key-12345"
  }'
```

**预期响应**（不回显明文）:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "providerId": 1,
    "providerCode": "test_provider",
    "name": "Test API Key",
    "status": "ACTIVE",
    "createdAt": "2026-02-15T08:00:00",
    "updatedAt": "2026-02-15T08:00:00"
  }
}
```

**验证点**:
- ✅ 响应中**不包含** `apiKeyCipher` 字段
- ✅ 响应中**不包含** `apiKey` 字段

### 步骤 4: 立即测试 Resolve（无需重启）

**等待 1-2 秒**（确保数据已提交），然后：

```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "prefer": ["test_provider"]
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "mode": "direct",
    "capability": "segmentation",
    "direct": {
      "providerCode": "test_provider",
      "endpoint": "https://api.test.com/v1/segmentation",
      "auth": {
        "type": "api_key",
        "apiKey": "sk-test-key-12345"
      },
      "timeoutMs": 8000,
      "params": {
        "model": "test",
        "quality": "high"
      }
    }
  }
}
```

**验证点**:
- ✅ Resolve 立即生效（无需重启应用）
- ✅ 返回正确的 providerCode
- ✅ 返回正确的 endpoint
- ✅ API Key 正确解密

### 步骤 5: 测试更新功能

```bash
# 更新 Capability
curl -X PUT http://localhost:8089/api/v1/admin/providers/1/capabilities/1 \
  -H "Content-Type: application/json" \
  -d '{
    "endpoint": "https://api.test.com/v2/segmentation",
    "priority": 50
  }'
```

**等待 1-2 秒**，然后再次测试 Resolve：

```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{
    "capability": "segmentation",
    "prefer": ["test_provider"]
  }'
```

**验证点**:
- ✅ Resolve 使用更新后的 endpoint
- ✅ 更新立即生效（无需重启）

## 验证清单

### ✅ 创建功能
- [ ] 能够创建 Provider
- [ ] 能够创建 Capability
- [ ] 能够创建 API Key（接收明文，加密存储）

### ✅ Resolve 立即生效
- [ ] 创建 Provider/Capability/Key 后，Resolve 立即可用
- [ ] 更新 Capability 后，Resolve 立即使用新值
- [ ] 无需重启应用

### ✅ 安全验证
- [ ] API Key 创建响应中不回显明文
- [ ] API Key 列表响应中不回显明文
- [ ] API Key 详情响应中不回显明文

### ✅ 更新功能
- [ ] 能够更新 Provider
- [ ] 能够更新 Capability
- [ ] 更新后 Resolve 立即生效

## 常见问题

### Q1: Resolve 返回 400 错误

**可能原因**:
- Provider 没有对应的 API Key
- Capability 状态不是 ACTIVE
- Provider 状态不是 ACTIVE

**解决方案**:
- 检查 API Key 是否创建成功
- 检查 status 字段

### Q2: API Key 响应中包含 apiKeyCipher

**可能原因**:
- 使用了错误的 DTO
- Controller 返回了 Entity 而不是 DTO

**解决方案**:
- 检查 `ProviderAdminController` 是否使用 `ProviderApiKeyDTO.from()`

### Q3: Resolve 不立即生效

**可能原因**:
- 数据库事务未提交
- 缓存问题（如果使用了缓存）

**解决方案**:
- 等待 1-2 秒后重试
- 检查 Service 方法是否有 `@Transactional` 注解

## 测试报告模板

```
测试时间: 2026-02-15
测试人员: [Your Name]

测试结果:
- 创建 Provider: ✅ PASS
- 创建 Capability: ✅ PASS
- 创建 API Key: ✅ PASS
- Resolve 立即生效: ✅ PASS
- 更新功能: ✅ PASS
- API Key 不回显明文: ✅ PASS

结论: 所有功能正常，Resolve 立即生效，无需重启应用
```
