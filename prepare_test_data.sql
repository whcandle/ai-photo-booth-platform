-- 准备 ResolveService 增强功能测试数据

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- 1. 检查并准备 Providers
-- ============================================
SELECT 'Step 1: Checking Providers' as step;

-- 检查现有的 providers
SELECT id, code, name, status FROM model_providers WHERE code IN ('aliyun', 'volc');

-- ============================================
-- 2. 检查并准备 Provider Capabilities
-- ============================================
SELECT 'Step 2: Checking Provider Capabilities' as step;

-- 检查 aliyun 的 capability
SELECT 
  mp.code,
  pc.capability,
  pc.endpoint,
  pc.priority,
  pc.status
FROM provider_capabilities pc
JOIN model_providers mp ON pc.provider_id = mp.id
WHERE mp.code = 'aliyun' AND pc.capability = 'segmentation';

-- 检查 volc 的 capability
SELECT 
  mp.code,
  pc.capability,
  pc.endpoint,
  pc.priority,
  pc.status
FROM provider_capabilities pc
JOIN model_providers mp ON pc.provider_id = mp.id
WHERE mp.code = 'volc' AND pc.capability = 'segmentation';

-- 如果 volc 没有 capability，插入（假设 volc provider 的 id 需要查询）
-- 注意：请先查询 volc provider 的 id
-- SELECT id FROM model_providers WHERE code = 'volc';

-- 插入 volc 的 capability（如果不存在）
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms)
SELECT id, 'segmentation', 'https://api.volc.com/v1/segmentation', 'ACTIVE', 200, 10000
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE 
    endpoint = VALUES(endpoint),
    status = VALUES(status),
    priority = VALUES(priority),
    default_timeout_ms = VALUES(default_timeout_ms);

-- ============================================
-- 3. 检查并准备 Provider API Keys
-- ============================================
SELECT 'Step 3: Checking Provider API Keys' as step;

-- 检查 aliyun 的 API Key
SELECT 
  mp.code,
  pak.id,
  pak.name,
  pak.status
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE mp.code = 'aliyun' AND pak.status = 'ACTIVE';

-- 检查 volc 的 API Key
SELECT 
  mp.code,
  pak.id,
  pak.name,
  pak.status
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE mp.code = 'volc' AND pak.status = 'ACTIVE';

-- 插入 volc 的 API Key（如果不存在）
-- 注意：需要先使用加密工具生成加密值，替换 'YOUR_ENCRYPTED_VOLC_KEY' 为实际加密值
-- 可以使用 Base64 编码作为快速测试：TO_BASE64('sk-test-volc-key-12345')
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)
SELECT id, 'Volc Test API Key', TO_BASE64('sk-test-volc-key-12345'), 'ACTIVE'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    api_key_cipher = VALUES(api_key_cipher),
    status = VALUES(status);

-- ============================================
-- 4. 检查并准备 Merchants
-- ============================================
SELECT 'Step 4: Checking Merchants' as step;

-- 检查 merchant TEST001
SELECT id, code, name, status FROM merchants WHERE code = 'TEST001';

-- 插入 merchant（如果不存在）
INSERT INTO merchants (code, name, status)
VALUES ('TEST001', 'Test Merchant', 'ACTIVE')
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    status = VALUES(status);

-- ============================================
-- 5. 检查并准备 Routing Policies
-- ============================================
SELECT 'Step 5: Checking Routing Policies' as step;

-- 检查 GLOBAL policy
SELECT 
  id,
  scope,
  merchant_id,
  capability,
  prefer_providers_json,
  status
FROM capability_routing_policies
WHERE scope = 'GLOBAL' AND capability = 'segmentation';

-- 检查 MERCHANT policy
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
WHERE crp.scope = 'MERCHANT' AND crp.capability = 'segmentation';

-- 插入 GLOBAL policy（如果不存在）
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status)
VALUES ('GLOBAL', NULL, 'segmentation', '["aliyun", "volc"]', 'ACTIVE')
ON DUPLICATE KEY UPDATE 
    prefer_providers_json = VALUES(prefer_providers_json),
    status = VALUES(status);

-- 插入 MERCHANT policy（如果不存在）
-- 注意：merchant_id 需要根据实际查询结果修改
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status)
SELECT 'MERCHANT', id, 'segmentation', '["volc"]', 'ACTIVE'
FROM merchants WHERE code = 'TEST001'
ON DUPLICATE KEY UPDATE 
    prefer_providers_json = VALUES(prefer_providers_json),
    status = VALUES(status);

-- ============================================
-- 6. 验证所有测试数据
-- ============================================
SELECT 'Step 6: Verifying Test Data' as step;

-- 验证 providers
SELECT 'Providers' as type, COUNT(*) as count 
FROM model_providers WHERE code IN ('aliyun', 'volc');

-- 验证 capabilities
SELECT 'Capabilities' as type, COUNT(*) as count 
FROM provider_capabilities pc
JOIN model_providers mp ON pc.provider_id = mp.id
WHERE mp.code IN ('aliyun', 'volc') AND pc.capability = 'segmentation' AND pc.status = 'ACTIVE';

-- 验证 API Keys
SELECT 'API Keys' as type, COUNT(*) as count 
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE mp.code IN ('aliyun', 'volc') AND pak.status = 'ACTIVE';

-- 验证 merchants
SELECT 'Merchants' as type, COUNT(*) as count 
FROM merchants WHERE code = 'TEST001';

-- 验证 policies
SELECT 'Policies' as type, COUNT(*) as count 
FROM capability_routing_policies WHERE capability = 'segmentation';

-- 完整数据概览
SELECT 
  'Summary' as type,
  (SELECT COUNT(*) FROM model_providers WHERE code IN ('aliyun', 'volc')) as providers,
  (SELECT COUNT(*) FROM provider_capabilities pc JOIN model_providers mp ON pc.provider_id = mp.id WHERE mp.code IN ('aliyun', 'volc') AND pc.capability = 'segmentation' AND pc.status = 'ACTIVE') as capabilities,
  (SELECT COUNT(*) FROM provider_api_keys pak JOIN model_providers mp ON pak.provider_id = mp.id WHERE mp.code IN ('aliyun', 'volc') AND pak.status = 'ACTIVE') as api_keys,
  (SELECT COUNT(*) FROM merchants WHERE code = 'TEST001') as merchants,
  (SELECT COUNT(*) FROM capability_routing_policies WHERE capability = 'segmentation') as policies;
