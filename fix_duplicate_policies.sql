-- 修复重复的 Routing Policies

-- 1. 检查重复记录
SELECT 'Checking for duplicate policies...' as step;

SELECT 
  scope,
  merchant_id,
  capability,
  COUNT(*) as count
FROM capability_routing_policies
WHERE capability = 'segmentation'
GROUP BY scope, merchant_id, capability
HAVING COUNT(*) > 1;

-- 2. 查看所有 GLOBAL policies
SELECT 'All GLOBAL policies:' as step;
SELECT 
  id,
  scope,
  merchant_id,
  capability,
  prefer_providers_json,
  status,
  created_at
FROM capability_routing_policies
WHERE scope = 'GLOBAL' AND capability = 'segmentation'
ORDER BY created_at DESC;

-- 3. 删除重复的 GLOBAL policy（保留最新的，即 created_at 最大的）
-- 注意：执行前请先备份数据
DELETE crp1 FROM capability_routing_policies crp1
INNER JOIN capability_routing_policies crp2
WHERE crp1.id < crp2.id
  AND crp1.scope = 'GLOBAL'
  AND crp1.capability = 'segmentation'
  AND crp1.merchant_id IS NULL
  AND crp2.scope = 'GLOBAL'
  AND crp2.capability = 'segmentation'
  AND crp2.merchant_id IS NULL;

-- 4. 验证修复结果
SELECT 'Verifying fix...' as step;
SELECT 
  scope,
  merchant_id,
  capability,
  COUNT(*) as count
FROM capability_routing_policies
WHERE capability = 'segmentation'
GROUP BY scope, merchant_id, capability
HAVING COUNT(*) > 1;

-- 如果上面的查询返回空，说明没有重复记录了
