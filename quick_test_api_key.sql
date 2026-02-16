-- 快速测试 API Key 的 SQL 脚本
-- 注意：需要先使用 EncryptApiKeyTool 生成加密后的值

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- 步骤 1: 检查现有的 Providers
-- ============================================
SELECT 'Step 1: 检查 Providers' as step;
SELECT id, code, name, status FROM model_providers WHERE code IN ('aliyun', 'volc');

-- ============================================
-- 步骤 2: 插入加密的 API Key
-- ============================================
-- 注意：将 'YOUR_ENCRYPTED_KEY_HERE' 替换为 EncryptApiKeyTool 生成的加密值
-- 示例明文: sk-test-aliyun-key-12345

SELECT 'Step 2: 插入 API Key（请先替换加密值）' as step;

-- 为 aliyun 插入 API Key
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun Test API Key', 'YOUR_ENCRYPTED_KEY_HERE', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun'
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    api_key_cipher = VALUES(api_key_cipher),
    status = VALUES(status);

-- 为 volc 插入 API Key（可选）
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Volc Test API Key', 'YOUR_ENCRYPTED_KEY_HERE', 'ACTIVE'
FROM model_providers WHERE code = 'volc'
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    api_key_cipher = VALUES(api_key_cipher),
    status = VALUES(status);

-- ============================================
-- 步骤 3: 验证插入的数据
-- ============================================
SELECT 'Step 3: 验证 API Keys' as step;
SELECT 
  pak.id,
  mp.code as provider_code,
  mp.name as provider_name,
  pak.name as key_name,
  LEFT(pak.api_key_cipher, 20) as api_key_cipher_preview,
  pak.status,
  pak.created_at
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE pak.status = 'ACTIVE'
ORDER BY mp.code, pak.created_at DESC;

-- ============================================
-- 步骤 4: 测试场景 - 禁用 API Key（用于测试无 Key 场景）
-- ============================================
-- 取消注释下面的 SQL 来禁用 API Key
-- UPDATE provider_api_keys 
-- SET status = 'INACTIVE' 
-- WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');

-- ============================================
-- 步骤 5: 恢复 API Key（测试完成后）
-- ============================================
-- UPDATE provider_api_keys 
-- SET status = 'ACTIVE' 
-- WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');
