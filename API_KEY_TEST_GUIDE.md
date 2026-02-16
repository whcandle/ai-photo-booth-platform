# API Key 功能测试指南

## 功能说明

ResolveService 现在支持：
1. 自动查询 provider 的 ACTIVE API Key（取最新的一条）
2. 使用 AES 加密解密 API Key
3. 在响应中返回解密后的明文 API Key
4. 如果找不到 API Key，返回 400 错误，错误码：`NO_ACTIVE_API_KEY`
5. 明文 API Key 不会写入日志

## 测试步骤

### 1. 准备加密的 API Key

由于 API Key 需要加密存储，需要先使用 CryptoUtil 加密。

**方法 1：使用 Java 代码加密**

创建一个临时测试类：

```java
import com.mg.platform.common.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EncryptApiKey implements CommandLineRunner {
    @Autowired
    private CryptoUtil cryptoUtil;
    
    public static void main(String[] args) {
        SpringApplication.run(EncryptApiKey.class, args);
    }
    
    @Override
    public void run(String... args) {
        String plainKey = "sk-test-aliyun-key-12345";
        String encrypted = cryptoUtil.encrypt(plainKey);
        System.out.println("Plain: " + plainKey);
        System.out.println("Encrypted: " + encrypted);
    }
}
```

**方法2：使用 Base64 简单编码（仅用于测试）**

```sql
-- 简单测试：使用 Base64 编码（不是真正的加密，仅用于测试）
-- 实际生产环境必须使用 CryptoUtil.encrypt()
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Test API Key', TO_BASE64('sk-test-key-12345'), 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
```

### 2. 插入测试数据

```sql
-- 使用加密后的 API Key 插入
-- 注意：需要先运行加密工具获取加密后的值
INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) 
SELECT id, 'Aliyun API Key', '<加密后的值>', 'ACTIVE'
FROM model_providers WHERE code = 'aliyun';
```

### 3. 测试场景

#### 场景 1: 有 API Key 的情况

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation","prefer":["aliyun"]}'
```

**预期响应**:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "mode": "direct",
    "capability": "segmentation",
    "direct": {
      "providerCode": "aliyun,
      "endpoint": "https://api.aliyun.com/v1/segmentation",
      "auth": {
        "type": "api_key",
        "apiKey": "sk-test-aliyun-key-12345"  // 解密后的明文
      },
      "timeoutMs": 8000,
      "params": {...}
    }
  }
}
```

#### 场景 2: 无 API Key 的情况

**准备**：删除或禁用 provider 的所有 API Key

```sql
-- 禁用 API Key（用于测试）
UPDATE provider_api_keys SET status = 'INACTIVE' WHERE provider_id = (SELECT id FROM model_providers WHERE code = 'aliyun');
```

**请求**:
```bash
curl -X POST http://localhost:8089/api/v1/ai/resolve \
  -H "Content-Type: application/json" \
  -d '{"capability":"segmentation","prefer":["aliyun"]}'
```

**预期响应**:
```json
{
  "success": false,
  "message": "NO_ACTIVE_API_KEY: No active API key found for provider",
  "data": null
}
```

**HTTP 状态码**: 400 Bad Request

### 4. 验证日志

检查应用日志，确认：
- ✅ 明文 API Key **不会**出现在日志中
- ✅ 只有 provider ID 和错误信息出现在日志中
- ✅ 解密失败时只记录错误，不记录明文

### 5. 单元测试

运行单元测试验证功能：

```bash
mvn test -Dtest=ResolveServiceTest
```

测试覆盖：
- ✅ `testResolve_WithApiKey_Success`: 有 API Key 的情况
- ✅ `testResolve_NoApiKey_ThrowsException`: 无 API Key 的情况

## 配置说明

### application.yml

```yaml
crypto:
  api-key:
    secret: default-secret-key-16  # 16 bytes for AES-128, 生产环境必须修改
```

**重要**：生产环境必须修改 `crypto.api-key.secret` 为安全的密钥（16 字节）。

## 安全注意事项

1. **密钥管理**：生产环境必须使用安全的密钥管理方案（如 AWS KMS、HashiCorp Vault）
2. **日志安全**：确保明文 API Key 不会出现在日志中
3. **传输安全**：API Key 在响应中返回，确保使用 HTTPS
4. **存储安全**：数据库中的 `api_key_cipher` 字段已加密

## 故障排查

1. **解密失败**：检查 `crypto.api-key.secret` 配置是否正确
2. **找不到 API Key**：检查 `provider_api_keys` 表中是否有 `status='ACTIVE'` 的记录
3. **400 错误**：检查错误消息是否包含 `NO_ACTIVE_API_KEY` 错误码
