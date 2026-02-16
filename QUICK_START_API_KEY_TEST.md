# API Key 测试快速开始指南

## 🚀 5 步完成测试

### 步骤 1: 生成加密的 API Key（2分钟）

**方法 A: 使用 Maven 运行加密工具**

```bash
cd D:\workspace\ai-photo-booth-platform
mvn spring-boot:run -Dspring-boot.run.main-class=com.mg.platform.util.EncryptApiKeyTool -Dspring-boot.run.arguments="sk-test-aliyun-key-12345"
```

**方法 B: 使用 Java 直接运行**

```bash
# 先编译
mvn clean compile

# 运行（需要设置 classpath，较复杂，推荐方法 A）
```

**输出示例**:
```
========================================
API Key Encryption Tool
========================================
Plain API Key: sk-test-aliyun-key-12345
Encrypted (Base64): dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=
========================================

SQL Insert Statement:
-- For aliyun:
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)
SELECT id, 'Aliyun Test API Key', 'dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
========================================
```

**复制加密后的值**（如：`dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=`）

### 步骤 2: 插入到数据库（1分钟）

```bash
mysql -u root -p ai_photo_booth
```

```sql
-- 将 'YOUR_ENCRYPTED_KEY' 替换为步骤 1 生成的加密值
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun Test API Key', 'YOUR_ENCRYPTED_KEY', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';

-- 验证
SELECT 
  mp.code,
  pak.name,
  LEFT(pak.api_key_cipher, 30) as encrypted_key_preview,
  pak.status
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE mp.code = 'aliyun' AND pak.status = 'ACTIVE';
```

### 步骤 3: 启动应用（如果未启动）

```bash
cd D:\workspace\ai-photo-booth-platform
mvn spring-boot:run
```

### 步骤 4: 测试 API（30秒）

**PowerShell**:
```powershell
$body = @{
    capability = "segmentation"
    prefer = @("aliyun")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8089/api/v1/ai/resolve" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body | ConvertTo-Json -Depth 10
```

**或使用 curl**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d "{\"capability\":\"segmentation\",\"prefer\":[\"aliyun\"]}"
```

### 步骤 5: 验证结果

**✅ 成功标志**:
- `success: true`
- `data.direct.auth.apiKey` 包含解密后的明文（如：`sk-test-aliyun-key-12345`）
- `data.direct.providerCode` 为 `aliyun`

**预期响应**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "mode": "direct",
    "capability": "segmentation",
    "direct": {
      "providerCode": "aliyun",
      "endpoint": "https://api.aliyun.com/v1/segmentation",
      "auth": {
        "type": "api_key",
        "apiKey": "sk-test-aliyun-key-12345"  // ✅ 解密后的明文
      },
      "timeoutMs": 8000,
      "params": {...}
    }
  }
}
```

## 📋 完整测试清单

### 测试场景 1: 有 API Key ✅

- [ ] 插入加密的 API Key
- [ ] 调用 API
- [ ] 验证返回解密后的明文
- [ ] 检查日志中不包含明文

### 测试场景 2: 无 API Key ❌

```sql
-- 禁用 API Key
UPDATE provider_api_keys 
SET status = 'INACTIVE' 
WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');
```

- [ ] 调用 API
- [ ] 验证返回 400 错误
- [ ] 验证错误码为 `NO_ACTIVE_API_KEY`

**预期错误响应**:
```json
{
  "success": false,
  "message": "NO_ACTIVE_API_KEY: No active API key found for provider",
  "data": null
}
```

## 🔧 故障排查

### 问题: 加密工具无法运行

**解决方案**: 直接使用 SQL 插入（仅用于测试）

```sql
-- 简单测试：使用 Base64 编码（不是真正的加密）
-- 注意：这仅用于快速测试，生产环境必须使用真正的加密
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Test Key', TO_BASE64('sk-test-aliyun-key-12345'), 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
```

**然后修改 CryptoUtil** 临时支持 Base64 解码（仅测试用）:
```java
// 临时测试：如果是 Base64 编码的明文，直接解码
try {
    String decoded = new String(Base64.getDecoder().decode(cipherText));
    if (decoded.startsWith("sk-")) {
        return decoded; // 假设是 Base64 编码的测试数据
    }
} catch (Exception e) {
    // 继续正常解密流程
}
```

### 问题: 解密失败

1. 检查 `application.yml` 中的 `crypto.api-key.secret` 配置
2. 确认加密和解密使用相同的密钥
3. 重新生成加密值并插入

### 问题: 找不到 API Key

1. 检查数据库：`SELECT * FROM provider_api_keys WHERE status='ACTIVE'`
2. 确认 provider_id 正确
3. 检查 SQL 插入是否成功

## 📝 一键测试脚本

运行完整测试脚本：

```powershell
cd D:\workspace\ai-photo-booth-platform
.\test_api_key_full.ps1
```

## ✅ 完成标志

- [x] API Key 成功插入数据库
- [x] API 返回解密后的明文
- [x] 无 API Key 时返回正确错误
- [x] 日志中不包含明文
- [x] 所有单元测试通过

---

**详细文档**: 查看 `API_KEY_FULL_TEST_GUIDE.md` 获取完整测试指南
