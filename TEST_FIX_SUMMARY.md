# 测试问题修复总结

## 测试结果分析

根据调试脚本的输出：

### ✅ 测试 1: Global Policy - PASS
- Provider Code: aliyun
- 说明 GLOBAL policy 正常工作

### ❌ 测试 2: Request Prefer Override Policy - FAIL
- **错误信息**: `NO_ACTIVE_API_KEY: No active API key found for provider`
- **原因**: `volc` provider 存在但没有对应的 API Key (status='ACTIVE')
- **解决方案**: 需要为 volc provider 插入 API Key

### ❌ 测试 3: Merchant Policy - FAIL
- **预期**: volc (MERCHANT policy first)
- **实际**: aliyun
- **原因**: MERCHANT policy 没有生效，回退到 GLOBAL policy
- **可能原因**:
  1. merchantCode='TEST001' 对应的 merchant 不存在
  2. MERCHANT policy 不存在或配置不正确
  3. MERCHANT policy 的 status 不是 'ACTIVE'

### ✅ 测试 4: Check Available Providers - PASS
- Current Provider: aliyun
- 说明基本功能正常

## 修复步骤

### 步骤 1: 执行数据准备脚本

```bash
mysql -u root -p ai_photo_booth < prepare_test_data.sql
```

这个脚本会：
1. 检查并确保 volc provider 有 segmentation capability
2. 为 volc provider 插入 API Key（使用 Base64 编码作为快速测试）
3. 检查并创建 merchant TEST001
4. 检查并创建 MERCHANT policy

### 步骤 2: 验证数据

执行 `check_test_data.sql` 验证所有数据是否正确：

```bash
mysql -u root -p ai_photo_booth < check_test_data.sql
```

### 步骤 3: 重新运行测试

```powershell
.\test_resolve_enhancement_debug.ps1
```

## 预期修复后的结果

修复后，所有测试应该通过：

1. ✅ **测试 1**: Global Policy - 继续通过
2. ✅ **测试 2**: Request Prefer Override Policy - 应该通过（volc 有 API Key）
3. ✅ **测试 3**: Merchant Policy - 应该通过（MERCHANT policy 正确配置）
4. ✅ **测试 4**: Check Available Providers - 继续通过

## 注意事项

1. **API Key 加密**: 
   - 脚本中使用 `TO_BASE64('sk-test-volc-key-12345')` 作为快速测试
   - 生产环境应使用真正的 AES 加密（参考 `CryptoUtil.encrypt()`）
   - 由于 `CryptoUtil` 支持 Base64 解码（临时方案），所以可以工作

2. **Merchant ID**: 
   - MERCHANT policy 的插入使用子查询自动获取 merchant_id
   - 如果 merchant 不存在，会先创建

3. **数据完整性**:
   - 确保所有 provider 都有对应的 capability 和 API Key
   - 确保 policy 的 status='ACTIVE'
   - 确保 prefer_providers_json 格式正确（JSON 数组）

## 快速修复命令

```sql
-- 快速修复：插入 volc API Key 和 MERCHANT policy
-- 1. 插入 volc API Key
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)
SELECT id, 'Volc Test API Key', TO_BASE64('sk-test-volc-key-12345'), 'ACTIVE'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE status='ACTIVE';

-- 2. 确保 merchant 存在
INSERT INTO merchants (code, name, status)
VALUES ('TEST001', 'Test Merchant', 'ACTIVE')
ON DUPLICATE KEY UPDATE status='ACTIVE';

-- 3. 插入 MERCHANT policy
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status)
SELECT 'MERCHANT', id, 'segmentation', '["volc"]', 'ACTIVE'
FROM merchants WHERE code = 'TEST001'
ON DUPLICATE KEY UPDATE prefer_providers_json='["volc"]', status='ACTIVE';
```

执行这些 SQL 后，重新运行测试脚本即可。
