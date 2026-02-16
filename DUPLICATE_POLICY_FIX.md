# 重复 Policy 问题修复

## 问题描述

测试时出现错误：`Query did not return a unique result: 2 results were returned`

**原因**：数据库中存在重复的 GLOBAL policy 记录（scope='GLOBAL', merchant_id=NULL, capability='segmentation'）

## 解决方案

### 方案 1: 代码修复（已实施）✅

修改了 `CapabilityRoutingPolicyRepository` 和 `ResolveService`：
- 使用 `findFirstByScopeAndMerchantIdAndCapabilityOrderByCreatedAtDesc` 方法
- 当存在多条记录时，返回最新的一条（按 created_at DESC）

**优点**：
- 代码层面处理，更健壮
- 即使有重复记录也能正常工作

### 方案 2: 数据库清理（推荐）

执行 `fix_duplicate_policies.sql` 清理重复记录：

```bash
mysql -u root -p ai_photo_booth < fix_duplicate_policies.sql
```

或者手动执行：

```sql
-- 删除重复的 GLOBAL policy（保留最新的）
DELETE crp1 FROM capability_routing_policies crp1
INNER JOIN capability_routing_policies crp2
WHERE crp1.id < crp2.id
  AND crp1.scope = 'GLOBAL'
  AND crp1.capability = 'segmentation'
  AND crp1.merchant_id IS NULL
  AND crp2.scope = 'GLOBAL'
  AND crp2.capability = 'segmentation'
  AND crp2.merchant_id IS NULL;
```

## 验证

### 1. 检查重复记录

```sql
SELECT 
  scope,
  merchant_id,
  capability,
  COUNT(*) as count
FROM capability_routing_policies
WHERE capability = 'segmentation'
GROUP BY scope, merchant_id, capability
HAVING COUNT(*) > 1;
```

如果返回空，说明没有重复记录。

### 2. 重新运行测试

```powershell
.\test_resolve_enhancement_debug.ps1
```

## 预防措施

1. **唯一约束**：表已有唯一约束 `uk_scope_capability (scope, merchant_id, capability)`，应该能防止重复
2. **代码层面**：已使用 `findFirstBy...` 方法，即使有重复也能正常工作
3. **数据插入**：使用 `ON DUPLICATE KEY UPDATE` 避免重复插入

## 建议

1. **立即执行**：运行 `fix_duplicate_policies.sql` 清理重复记录
2. **验证**：重新运行测试，应该所有测试通过
3. **监控**：如果再次出现重复，检查数据插入逻辑
