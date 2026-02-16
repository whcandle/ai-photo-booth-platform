# API Key 完整测试指南

## 测试目标

测试 ResolveService 的 API Key 查询和解密功能，验证：
1. 能够正确查询并解密 API Key
2. 在响应中返回解密后的明文
3. 无 API Key 时返回正确的错误码

## 前置条件

1. MySQL 8.0 已启动
2. 数据库 `ai_photo_booth` 已创建
3. Flyway 迁移已执行（包括 V6）
4. Spring Boot 应用已启动（端口 8089）
5. 测试数据已准备（model_providers 和 provider_capabilities）

## 步骤 1: 准备加密工具类（临时）

创建一个临时的加密工具类来生成加密后的 API Key。

### 1.1 创建临时加密工具

创建文件：`src/main/java/com/mg/platform/util/EncryptApiKeyTool.java`

```java
package com.mg.platform.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SpringBootApplication
@ComponentScan(basePackages = "com.mg.platform")
public class EncryptApiKeyTool implements CommandLineRunner {
    
    @Value("${crypto.api-key.secret:default-secret-key-16}")
    private String secretKey;
    
    public static void main(String[] args) {
        SpringApplication.run(EncryptApiKeyTool.class, args);
    }
    
    @Override
    public void run(String... args) {
        if (args.length == 0) {
            System.out.println("Usage: java EncryptApiKeyTool <plain-api-key>");
            System.out.println("Example: java EncryptApiKeyTool sk-test-aliyun-key-12345");
            return;
        }
        
        String plainKey = args[0];
        String encrypted = encrypt(plainKey);
        
        System.out.println("========================================");
        System.out.println("API Key Encryption Tool");
        System.out.println("========================================");
        System.out.println("Plain API Key: " + plainKey);
        System.out.println("Encrypted (Base64): " + encrypted);
        System.out.println("========================================");
        System.out.println("\nSQL Insert Statement:");
        System.out.println("INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)");
        System.out.println("SELECT id, 'Test API Key', '" + encrypted + "', 'ACTIVE'");
        System.out.println("FROM model_providers WHERE code = 'aliyun';");
        System.out.println("========================================");
    }
    
    private String encrypt(String plainText) {
        try {
            byte[] keyBytes = getKeyBytes();
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }
    
    private byte[] getKeyBytes() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        return key;
    }
}
```

### 1.2 运行加密工具

```bash
cd D:\workspace\ai-photo-booth-platform
mvn spring-boot:run -Dspring-boot.run.arguments="sk-test-aliyun-key-12345"
```

或者直接运行 main 方法，传入参数。

**输出示例**:
```
========================================
API Key Encryption Tool
========================================
Plain API Key: sk-test-aliyun-key-12345
Encrypted (Base64): dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=
========================================

SQL Insert Statement:
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)
SELECT id, 'Test API Key', 'dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
========================================
```

## 步骤 2: 插入加密的 API Key 到数据库

### 2.1 连接到 MySQL

```bash
mysql -u root -p ai_photo_booth
```

### 2.2 检查现有的 Provider

```sql
-- 查看现有的 providers
SELECT id, code, name, status FROM model_providers WHERE code IN ('aliyun', 'volc');
```

**预期输出**（示例）:
```
+----+--------+--------+--------+
| id | code   | name   | status |
+----+--------+--------+--------+
|  2 | aliyun | 阿里云 | ACTIVE |
|  3 | volc   | 火山引擎 | ACTIVE |
+----+--------+--------+--------+
```

### 2.3 插入加密的 API Key

使用步骤 1.2 生成的加密值，执行 SQL：

```sql
-- 为 aliyun provider 插入 API Key
-- 注意：将 'YOUR_ENCRYPTED_KEY_HERE' 替换为步骤 1.2 生成的加密值
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun Test API Key', 'YOUR_ENCRYPTED_KEY_HERE', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';

-- 为 volc provider 插入 API Key（可选，用于测试多个 provider）
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Volc Test API Key', 'YOUR_ENCRYPTED_KEY_HERE', 'ACTIVE'
FROM model_providers WHERE code = 'volc';
```

**实际示例**（假设加密后的值为 `dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=`）:

```sql
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun Test API Key', 'dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4=', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
```

### 2.4 验证插入的数据

```sql
-- 查看插入的 API Keys
SELECT 
  pak.id,
  mp.code as provider_code,
  mp.name as provider_name,
  pak.name as key_name,
  pak.api_key_cipher,
  pak.status,
  pak.created_at
FROM provider_api_keys pak
JOIN model_providers mp ON pak.provider_id = mp.id
WHERE pak.status = 'ACTIVE'
ORDER BY mp.code, pak.created_at DESC;
```

**预期输出**:
```
+----+---------------+--------------+---------------------+----------------------------------+--------+---------------------+
| id | provider_code | provider_name| key_name            | api_key_cipher                   | status | created_at          |
+----+---------------+--------------+---------------------+----------------------------------+--------+---------------------+
|  1 | aliyun        | 阿里云       | Aliyun Test API Key | dGVzdC1lbmNyeXB0ZWQta2V5LWZvci1hbGl5dW4= | ACTIVE | 2026-02-15 07:50:00 |
+----+---------------+--------------+---------------------+----------------------------------+--------+---------------------+
```

## 步骤 3: 测试 API（有 API Key 的情况）

### 3.1 使用 curl 测试

```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d "{\"capability\":\"segmentation\",\"prefer\":[\"aliyun\"]}"
```

### 3.2 使用 PowerShell 测试

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

### 3.3 预期响应

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
        "apiKey": "sk-test-aliyun-key-12345"  // 解密后的明文
      },
      "timeoutMs": 8000,
      "params": {
        "model": "sam",
        "quality": "high"
      }
    }
  }
}
```

### 3.4 验证要点

✅ **检查 auth.apiKey**：应该是解密后的明文（`sk-test-aliyun-key-12345`）  
✅ **检查 providerCode**：应该是 `aliyun`  
✅ **检查 endpoint**：应该是正确的 endpoint  
✅ **检查日志**：确认明文 API Key **不会**出现在日志中

## 步骤 4: 测试无 API Key 的情况

### 4.1 禁用 API Key

```sql
-- 禁用 aliyun 的所有 API Keys
UPDATE provider_api_keys 
SET status = 'INACTIVE' 
WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');
```

### 4.2 测试 API

```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d "{\"capability\":\"segmentation\",\"prefer\":[\"aliyun\"]}"
```

### 4.3 预期响应

```json
{
  "success": false,
  "message": "NO_ACTIVE_API_KEY: No active API key found for provider",
  "data": null
}
```

**HTTP 状态码**: `400 Bad Request`

### 4.4 恢复 API Key（用于后续测试）

```sql
-- 恢复 API Key
UPDATE provider_api_keys 
SET status = 'ACTIVE' 
WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');
```

## 步骤 5: 完整测试脚本

### 5.1 PowerShell 完整测试脚本

创建 `test_api_key_full.ps1`:

```powershell
# API Key 完整测试脚本

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Key 功能完整测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 测试 1: 有 API Key 的情况
Write-Host "=== 测试 1: 有 API Key（aliyun） ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
    prefer = @("aliyun")
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1
    Write-Host "✅ 成功" -ForegroundColor Green
    Write-Host "Provider Code: $($response1.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "API Key Type: $($response1.data.direct.auth.type)" -ForegroundColor Yellow
    Write-Host "API Key (前10字符): $($response1.data.direct.auth.apiKey.Substring(0, [Math]::Min(10, $response1.data.direct.auth.apiKey.Length)))..." -ForegroundColor Yellow
    $response1 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "❌ 失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host "错误响应: " -NoNewline
        $errorJson | ConvertTo-Json -Depth 10
    }
}

Write-Host "`n" -NoNewline

# 测试 2: 测试 volc（如果有 API Key）
Write-Host "=== 测试 2: 有 API Key（volc） ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc")
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2
    Write-Host "✅ 成功" -ForegroundColor Green
    Write-Host "Provider Code: $($response2.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "API Key: $($response2.data.direct.auth.apiKey.Substring(0, [Math]::Min(10, $response2.data.direct.auth.apiKey.Length)))..." -ForegroundColor Yellow
} catch {
    Write-Host "❌ 失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host "错误响应: " -NoNewline
        $errorJson | ConvertTo-Json -Depth 10
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
```

### 5.2 运行测试脚本

```powershell
cd D:\workspace\ai-photo-booth-platform
.\test_api_key_full.ps1
```

## 步骤 6: 验证日志安全性

### 6.1 检查应用日志

查看应用控制台或日志文件，确认：

✅ **不应该出现**：
- 明文 API Key（如 `sk-test-aliyun-key-12345`）
- 完整的解密后的 API Key

✅ **应该出现**：
- Provider ID（如 `provider ID: 2`）
- 错误信息（如 `No active API key found for provider ID: 2`）

### 6.2 日志示例（正确）

```
INFO  - ResolveService: Resolving capability: segmentation
WARN  - ResolveService: No active API key found for provider ID: 2
```

### 6.3 日志示例（错误 - 不应该出现）

```
INFO  - API Key decrypted: sk-test-aliyun-key-12345  // ❌ 不应该出现
```

## 步骤 7: 快速测试命令（一键执行）

### 7.1 完整测试流程

```bash
# 1. 启动应用（如果未启动）
cd D:\workspace\ai-photo-booth-platform
mvn spring-boot:run

# 2. 在另一个终端，测试 API
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d "{\"capability\":\"segmentation\",\"prefer\":[\"aliyun\"]}"
```

### 7.2 验证 SQL 查询

```sql
-- 快速验证 API Key 是否存在
SELECT 
  mp.code,
  COUNT(pak.id) as active_key_count
FROM model_providers mp
LEFT JOIN provider_api_keys pak ON mp.id = pak.provider_id AND pak.status = 'ACTIVE'
WHERE mp.code IN ('aliyun', 'volc')
GROUP BY mp.code;
```

## 常见问题排查

### 问题 1: 解密失败

**错误信息**: `Failed to decrypt API key`

**解决方案**:
1. 检查 `application.yml` 中的 `crypto.api-key.secret` 配置
2. 确认加密时使用的密钥与解密时使用的密钥一致
3. 重新加密并插入 API Key

### 问题 2: 找不到 API Key

**错误信息**: `NO_ACTIVE_API_KEY: No active API key found for provider`

**解决方案**:
1. 检查 `provider_api_keys` 表中是否有 `status='ACTIVE'` 的记录
2. 确认 `provider_id` 正确
3. 检查 SQL 查询是否正确

### 问题 3: API Key 为空

**现象**: 响应中 `auth.apiKey` 为空字符串

**解决方案**:
1. 检查加密过程是否正确
2. 验证数据库中的 `api_key_cipher` 字段值是否正确
3. 检查解密逻辑是否正常

## 测试检查清单

- [ ] 已创建加密工具并生成加密值
- [ ] 已插入加密的 API Key 到数据库
- [ ] 已验证数据库中的数据
- [ ] 已测试有 API Key 的情况（返回解密后的明文）
- [ ] 已测试无 API Key 的情况（返回 400 错误和错误码）
- [ ] 已验证日志中不包含明文 API Key
- [ ] 已验证错误码为 `NO_ACTIVE_API_KEY`
- [ ] 已验证 HTTP 状态码正确（200 或 400）

## 完成标志

✅ 所有测试通过  
✅ API Key 正确解密并返回  
✅ 错误处理正确  
✅ 日志安全（不包含明文）  
✅ 单元测试通过
