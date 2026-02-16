-- 检查重复的 Routing Policies

-- 检查 GLOBAL policy 是否有重复
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
ORDER BY created_at;

-- 检查 MERCHANT policy 是否有重复
SELECT 
  crp.id,
  crp.scope,
  crp.merchant_id,
  m.code as merchant_code,
  crp.capability,
  crp.prefer_providers_json,
  crp.status,
  crp.created_at
FROM capability_routing_policies crp
LEFT JOIN merchants m ON crp.merchant_id = m.id
WHERE crp.scope = 'MERCHANT' AND crp.capability = 'segmentation'
ORDER BY crp.merchant_id, crp.created_at;

-- 检查是否有违反唯一约束的记录
SELECT 
  scope,
  merchant_id,
  capability,
  COUNT(*) as count
FROM capability_routing_policies
WHERE capability = 'segmentation'
GROUP BY scope, merchant_id, capability
HAVING COUNT(*) > 1;

-- 清理重复的 GLOBAL policy（保留最新的）
-- 注意：执行前请先备份数据
-- DELETE crp1 FROM capability_routing_policies crp1
-- INNER JOIN capability_routing_policies crp2
-- WHERE crp1.id < crp2.id
--   AND crp1.scope = 'GLOBAL'
--   AND crp1.capability = 'segmentation'
--   AND crp2.scope = 'GLOBAL'
--   AND crp2.capability = 'segmentation';
