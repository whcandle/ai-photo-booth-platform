-- Resolve API 测试数据准备脚本
-- 执行前请确保 Flyway 迁移已完成（包括 V6）

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- 1. 插入 Model Providers
-- ============================================
INSERT INTO model_providers (code, name, status) VALUES
  ('aliyun', '阿里云', 'ACTIVE'),
  ('volc', '火山引擎', 'ACTIVE'),
  ('local_sd', '本地 Stable Diffusion', 'ACTIVE'),
  ('rembg', 'Remove Background', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

-- ============================================
-- 2. 插入 Provider Capabilities
-- ============================================
-- 注意：需要根据实际插入的 provider ID 调整 provider_id
-- 这里使用子查询获取 ID

-- segmentation capability
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'segmentation', 'https://api.aliyun.com/v1/segmentation', 'ACTIVE', 100, 8000, '{"model":"sam","quality":"high"}'
FROM model_providers WHERE code = 'aliyun'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'segmentation', 'https://api.volcengine.com/v1/segmentation', 'ACTIVE', 200, 10000, '{"model":"u2net","quality":"medium"}'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'segmentation', 'http://localhost:7860/api/segmentation', 'ACTIVE', 300, 15000, '{"model":"local_sam","quality":"high"}'
FROM model_providers WHERE code = 'local_sd'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'segmentation', 'https://api.rembg.com/v1/remove-bg', 'ACTIVE', 400, 5000, '{"model":"u2net"}'
FROM model_providers WHERE code = 'rembg'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

-- background_generation capability
INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'background_generation', 'https://api.aliyun.com/v1/bg-gen', 'ACTIVE', 100, 12000, '{"style":"realistic"}'
FROM model_providers WHERE code = 'aliyun'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

INSERT INTO provider_capabilities (provider_id, capability, endpoint, status, priority, default_timeout_ms, default_params_json) 
SELECT id, 'background_generation', 'https://api.volcengine.com/v1/bg-gen', 'ACTIVE', 200, 10000, '{"style":"artistic"}'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE endpoint=VALUES(endpoint), priority=VALUES(priority), default_timeout_ms=VALUES(default_timeout_ms), default_params_json=VALUES(default_params_json);

-- ============================================
-- 3. 插入 GLOBAL Routing Policy（可选）
-- ============================================
INSERT INTO capability_routing_policies (scope, merchant_id, capability, prefer_providers_json, status) VALUES
  ('GLOBAL', NULL, 'segmentation', '["aliyun","volc","local_sd"]', 'ACTIVE')
ON DUPLICATE KEY UPDATE prefer_providers_json=VALUES(prefer_providers_json), status=VALUES(status);

-- ============================================
-- 4. 验证数据
-- ============================================
SELECT 'Model Providers:' as info;
SELECT id, code, name, status FROM model_providers WHERE code IN ('aliyun', 'volc', 'local_sd', 'rembg');

SELECT 'Provider Capabilities:' as info;
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

SELECT 'Routing Policies:' as info;
SELECT id, scope, merchant_id, capability, prefer_providers_json, status 
FROM capability_routing_policies 
WHERE status = 'ACTIVE';
