-- 检查测试数据脚本

-- 1. 检查 providers 和 capabilities
SELECT '=== Providers and Capabilities ===' as section;
SELECT 
  mp.id as provider_id,
  mp.code as provider_code,
  mp.name as provider_name,
  pc.id as capability_id,
  pc.capability,
  pc.endpoint,
  pc.priority,
  pc.status as capability_status
FROM model_providers mp
LEFT JOIN provider_capabilities pc ON mp.id = pc.provider_id
WHERE pc.capability = 'segmentation' AND pc.status = 'ACTIVE'
ORDER BY mp.code, pc.priority;

-- 2. 检查 API Keys
SELECT '=== API Keys ===' as section;
SELECT 
  mp.code as provider_code,
  pak.id as key_id,
  pak.name as key_name,
  pak.status as key_status,
  pak.created_at
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE pak.status = 'ACTIVE'
ORDER BY mp.code, pak.created_at DESC;

-- 3. 检查 Routing Policies
SELECT '=== Routing Policies ===' as section;
SELECT 
  crp.id,
  crp.scope,
  crp.merchant_id,
  m.code as merchant_code,
  crp.capability,
  crp.prefer_providers_json,
  crp.status
FROM capability_routing_policies crp
LEFT JOIN merchants m ON crp.merchant_id = m.id
WHERE crp.capability = 'segmentation'
ORDER BY crp.scope, crp.merchant_id;

-- 4. 检查 Merchants
SELECT '=== Merchants ===' as section;
SELECT id, code, name, status
FROM merchants
WHERE code = 'TEST001' OR code LIKE 'TEST%'
ORDER BY code;

-- 5. 检查特定 provider 的完整信息
SELECT '=== Provider Details (aliyun) ===' as section;
SELECT 
  mp.id,
  mp.code,
  mp.name,
  COUNT(DISTINCT pc.id) as capability_count,
  COUNT(DISTINCT pak.id) as active_key_count
FROM model_providers mp
LEFT JOIN provider_capabilities pc ON mp.id = pc.provider_id AND pc.capability = 'segmentation' AND pc.status = 'ACTIVE'
LEFT JOIN provider_api_keys pak ON mp.id = pak.provider_id AND pak.status = 'ACTIVE'
WHERE mp.code = 'aliyun'
GROUP BY mp.id, mp.code, mp.name;

SELECT '=== Provider Details (volc) ===' as section;
SELECT 
  mp.id,
  mp.code,
  mp.name,
  COUNT(DISTINCT pc.id) as capability_count,
  COUNT(DISTINCT pak.id) as active_key_count
FROM model_providers mp
LEFT JOIN provider_capabilities pc ON mp.id = pc.provider_id AND pc.capability = 'segmentation' AND pc.status = 'ACTIVE'
LEFT JOIN provider_api_keys pak ON mp.id = pak.provider_id AND pak.status = 'ACTIVE'
WHERE mp.code = 'volc'
GROUP BY mp.id, mp.code, mp.name;
