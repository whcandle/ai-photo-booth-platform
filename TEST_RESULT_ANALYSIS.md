# 测试结果分析

## 测试结果

根据提供的测试输出：

1. ✅ **测试 1: Global Policy 生效** - PASS
   - Provider Code: aliyun
   - 说明 GLOBAL policy 正常工作

2. ❌ **测试 2: Request Prefer 覆盖 Policy** - 400 错误
   - 错误：远程服务器返回错误: (400) 错误的请求
   - 可能原因：`volc` provider 不存在或没有对应的 API Key

3. ❌ **测试 3: Merchant Policy 生效** - FAIL
   - 预期：volc (MERCHANT policy 第一个)
   - 实际：aliyun
   - 说明：MERCHANT policy 没有生效，回退到了 GLOBAL policy

4. ❌ **测试 4: 优先级测试** - 400 错误
   - 错误：远程服务器返回错误: (400) 错误的请求
   - 可能原因：`local_sd` provider 不存在或没有对应的 API Key

## 问题分析

### 问题 1: Request Prefer 返回 400 错误

**可能原因**：
1. `volc` provider 在 `provider_capabilities` 表中不存在
2. `volc` provider 存在但没有对应的 `provider_api_keys` (status='ACTIVE')
3. `volc` provider 的 capability 状态不是 'ACTIVE'

**解决方案**：
1. 检查数据库中是否存在 `volc` provider
2. 检查是否存在对应的 capability 和 API Key
3. 如果不存在，需要先插入测试数据

### 问题 2: Merchant Policy 没有生效

**可能原因**：
1. `merchantCode = "TEST001"` 对应的 merchant 不存在
2. MERCHANT policy 不存在（scope='MERCHANT', merchant_id=?, capability='segmentation'）
3. MERCHANT policy 的 status 不是 'ACTIVE'
4. MERCHANT policy 的 `prefer_providers_json` 为空或格式错误

**解决方案**：
1. 检查 merchants 表中是否存在 code='TEST001' 的记录
2. 检查 capability_routing_policies 表中是否存在对应的 MERCHANT policy
3. 确保 policy 的 status='ACTIVE' 且 prefer_providers_json 格式正确

### 问题 3: local_sd provider 返回 400 错误

**可能原因**：
- `local_sd` provider 不存在或没有 API Key

## 诊断步骤

### 步骤 1: 检查数据库数据

执行 `check_test_data.sql` 脚本：

```bash
mysql -u root -p ai_photo_booth < check_test_data.sql
```

### 步骤 2: 运行调试脚本

```powershell
.\test_resolve_enhancement_debug.ps1
```

### 步骤 3: 检查应用日志

查看应用控制台输出，查找：
- "Merchant not found for code: TEST001"
- "Some preferred providers not found in capabilities: [volc]"
- "NO_ACTIVE_API_KEY"

## 修复建议

### 1. 准备完整的测试数据

```sql
-- 确保 volc provider 存在且有 capability 和 API Key
-- 检查并插入 volc provider 的 capability
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms)
SELECT id, 'segmentation', 'https://api.volc.com/v1/segmentation', 'ACTIVE', 200, 10000
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), status=VALUES(status);

-- 检查并插入 volc provider 的 API Key
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)
SELECT id, 'Volc Test API Key', 'YOUR_ENCRYPTED_KEY', 'ACTIVE'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE status=VALUES(status);
```

### 2. 准备 Merchant Policy 测试数据

```sql
-- 确保 merchant 存在
INSERT INTO merchants (code, name, status)
VALUES ('TEST001', 'Test Merchant', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

-- 插入 MERCHANT policy（假设 merchant_id 为 1，请根据实际情况修改）
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status)
SELECT 'MERCHANT', id, 'segmentation', '["volc"]', 'ACTIVE'
FROM merchants WHERE code = 'TEST001'
ON DUPLICATE KEY UPDATE prefer_providers_json=VALUES(prefer_providers_json), status=VALUES(status);
```

### 3. 验证数据

```sql
-- 验证所有测试数据
SELECT 'Providers' as type, COUNT(*) as count FROM model_providers WHERE code IN ('aliyun', 'volc')
UNION ALL
SELECT 'Capabilities', COUNT(*) FROM provider_capabilities WHERE capability='segmentation' AND status='ACTIVE'
UNION ALL
SELECT 'API Keys', COUNT(*) FROM provider_api_keys WHERE status='ACTIVE'
UNION ALL
SELECT 'Policies', COUNT(*) FROM capability_routing_policies WHERE capability='segmentation';
```

## 改进建议

1. **增强错误信息**：已改进 `sortByPrefer` 方法，当 prefer 中的 provider 不存在时记录警告
2. **添加日志**：在关键位置添加日志，便于调试
3. **数据验证**：在测试前验证测试数据的完整性

## 下一步操作

1. 执行 `check_test_data.sql` 检查数据
2. 根据检查结果补充缺失的测试数据
3. 重新运行测试脚本
4. 如果仍有问题，查看应用日志获取详细错误信息
