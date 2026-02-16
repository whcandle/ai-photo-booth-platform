-- API Key 测试数据准备脚本
-- 注意：需要先使用 CryptoUtil 加密 API Key，然后插入到数据库
-- 这里提供一个示例，实际使用时需要先加密

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- 插入 Provider API Keys（需要先加密）
-- ============================================
-- 注意：api_key_cipher 需要使用 CryptoUtil.encrypt() 方法加密
-- 示例明文：sk-test-aliyun-key-12345
-- 加密后插入

-- 为 aliyun provider 插入 API Key
-- 假设 aliyun provider ID 为 2（根据实际调整）
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun API Key 1', 'dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun'
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

-- 为 volc provider 插入 API Key
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Volc API Key 1', 'dGVzdC1lbmNyeXB0ZWQta2V5LWZvci12b2xj', 'ACTIVE'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

-- ============================================
-- 验证数据
-- ============================================
SELECT 'Provider API Keys:' as info;
SELECT 
  pak.id,
  mp.code as provider_code,
  pak.name,
  pak.status,
  pak.created_at
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE pak.status = 'ACTIVE'
ORDER BY mp.code, pak.created_at DESC;
