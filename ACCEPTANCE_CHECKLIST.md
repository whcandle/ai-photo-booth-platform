# Admin CRUD API 验收检查清单

## 验收目标

- [x] Postman 能创建 provider/capability/key
- [x] Resolve 能立刻生效（不用重启）
- [x] 提供自动化测试脚本
- [x] 提供完整测试步骤

## 快速验收（3 步）

### 步骤 1: 启动应用

```bash
cd D:\workspace\ai-photo-booth-platform
mvn spring-boot:run
```

### 步骤 2: 运行自动化测试

```powershell
.\quick_test_admin_api.ps1
```

### 步骤 3: 查看测试结果

脚本会自动：
1. ✅ 创建 Provider
2. ✅ 创建 Capability  
3. ✅ 创建 API Key
4. ✅ 验证 Resolve 立即生效（无需重启）
5. ✅ 验证 API Key 不回显明文

## 详细验收步骤

### 方法 1: 自动化测试（推荐）

**快速测试**:
```powershell
.\quick_test_admin_api.ps1
```

**完整测试**:
```powershell
.\test_admin_crud_api.ps1
```

### 方法 2: Postman 手动测试

#### 1. 创建 Provider

```
POST http://localhost:8089/api/v1/admin/providers
Content-Type: application/json

{
  "code": "test_provider",
  "name": "Test Provider",
  "status": "ACTIVE"
}
```

**预期**: 返回创建的 Provider，记录 `id`

#### 2. 创建 Capability

```
POST http://localhost:8089/api/v1/admin/providers/{providerId}/capabilities
Content-Type: application/json

{
  "capability": "segmentation",
  "endpoint": "https://api.test.com/v1/segmentation",
  "status": "ACTIVE",
  "priority": 100,
  "defaultTimeoutMs": 8000
}
```

**预期**: 返回创建的 Capability

#### 3. 创建 API Key

```
POST http://localhost:8089/api/v1/admin/providers/{providerId}/keys
Content-Type: application/json

{
  "name": "Test API Key",
  "apiKey": "sk-test-key-12345"
}
```

**预期**: 
- ✅ 返回创建的 API Key
- ✅ **不包含** `apiKeyCipher` 字段
- ✅ **不包含** `apiKey` 字段

#### 4. 立即测试 Resolve（无需重启）

**等待 1-2 秒**，然后：

```
POST http://localhost:8089/api/v1/ai/resolve
Content-Type: application/json

{
  "capability": "segmentation",
  "prefer": ["test_provider"]
}
```

**预期**:
- ✅ 返回 `providerCode: "test_provider"`
- ✅ 返回 `endpoint: "https://api.test.com/v1/segmentation"`
- ✅ 返回解密后的 `apiKey: "sk-test-key-12345"`
- ✅ **无需重启应用**

### 方法 3: curl 命令行测试

```bash
# 1. 创建 Provider
curl -X POST http://localhost:8089/api/v1/admin/providers \
  -H "Content-Type: application/json" \
  -d '{"code":"test_provider","name":"Test Provider","status":"ACTIVE"}'

# 2. 创建 Capability（替换 {providerId}）
curl -X POST http://localhost:8089/api/v1/admin/providers/1/capabilities \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation","endpoint":"https://api.test.com/v1/segmentation","status":"ACTIVE","priority":100,"defaultTimeoutMs":8000}'

# 3. 创建 API Key（替换 {providerId}）
curl -X POST http://localhost:8089/api/v1/admin/providers/1/keys \
  -H "Content-Type: application/json" \
  -d '{"name":"Test API Key","apiKey":"sk-test-key-12345"}'

# 4. 等待 2 秒后测试 Resolve
sleep 2
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation","prefer":["test_provider"]}'
```

## 验收标准

### ✅ 必须通过

1. **创建功能**
   - [ ] 能够创建 Provider
   - [ ] 能够创建 Capability
   - [ ] 能够创建 API Key

2. **Resolve 立即生效**
   - [ ] 创建后立即调用 Resolve，能返回新创建的 Provider
   - [ ] 无需重启应用
   - [ ] API Key 正确解密

3. **安全验证**
   - [ ] API Key 创建响应中不回显明文
   - [ ] API Key 列表响应中不回显明文

### ✅ 可选验证

- [ ] 更新功能正常
- [ ] 列表功能正常
- [ ] 错误处理正确

## 测试结果示例

### 成功输出

```
========================================
Admin CRUD API 快速测试
========================================

[1/4] 创建 Provider...
  Provider ID: 1
  Provider Code: test_20260215084530

[2/4] 创建 Capability...
  Capability ID: 1

[3/4] 创建 API Key...
  API Key ID: 1
  验证: 响应中不包含明文 API Key

[4/4] 测试 Resolve 立即生效（无需重启）...
  Resolved Provider: test_20260215084530
  Endpoint: https://api.test.com/v1/segmentation
  验证: Resolve 立即生效！
  验证: API Key 正确解密

========================================
测试完成
========================================
结论:
  - Provider/Capability/Key 创建成功
  - Resolve 立即生效（无需重启）
  - API Key 不回显明文
========================================
```

## 故障排查

### 问题 1: 创建失败，返回 400

**检查**:
- 应用是否启动
- 数据库连接是否正常
- 请求体格式是否正确

### 问题 2: Resolve 不立即生效

**检查**:
- 等待时间是否足够（1-2 秒）
- Provider/Capability/Key 的 status 是否为 ACTIVE
- 数据库事务是否已提交

### 问题 3: API Key 响应中包含明文

**检查**:
- Controller 是否使用 `ProviderApiKeyDTO.from()`
- DTO 类是否正确排除 `apiKeyCipher` 字段

## 验收签字

- [ ] 创建功能测试通过
- [ ] Resolve 立即生效测试通过
- [ ] API Key 不回显明文验证通过
- [ ] 自动化测试脚本运行成功

**测试人员**: _______________  
**测试时间**: _______________  
**测试结果**: ✅ PASS / ❌ FAIL
