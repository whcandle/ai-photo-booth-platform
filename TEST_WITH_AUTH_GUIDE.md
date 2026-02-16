# Admin CRUD API 测试指南（含认证）

## 权限说明

Admin API 需要 ADMIN 角色权限。测试前需要先登录获取 token。

## 快速测试（2 种方式）

### 方式 1: 使用认证的测试脚本（推荐）

```powershell
.\quick_test_admin_api_with_auth.ps1
```

脚本会自动：
1. 登录获取 token
2. 使用 token 调用 Admin API
3. 测试所有功能

### 方式 2: 临时开放权限（仅测试用）

如果测试环境不需要认证，可以临时修改 `SecurityConfig.java`：

```java
// 临时开放（仅测试用）
.requestMatchers("/api/v1/admin/**").permitAll()
```

**注意**: 测试完成后应恢复为：
```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

## 手动测试步骤（Postman）

### 步骤 1: 登录获取 Token

```
POST http://localhost:8089/api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@platform.com",
  "password": "admin123"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {...}
  }
}
```

**记录**: `token` 值

### 步骤 2: 使用 Token 创建 Provider

```
POST http://localhost:8089/api/v1/admin/providers
Authorization: Bearer {token}
Content-Type: application/json

{
  "code": "test_provider",
  "name": "Test Provider",
  "status": "ACTIVE"
}
```

### 步骤 3: 创建 Capability

```
POST http://localhost:8089/api/v1/admin/providers/{providerId}/capabilities
Authorization: Bearer {token}
Content-Type: application/json

{
  "capability": "segmentation",
  "endpoint": "https://api.test.com/v1/segmentation",
  "status": "ACTIVE",
  "priority": 100,
  "defaultTimeoutMs": 8000
}
```

### 步骤 4: 创建 API Key

```
POST http://localhost:8089/api/v1/admin/providers/{providerId}/keys
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Test API Key",
  "apiKey": "sk-test-key-12345"
}
```

### 步骤 5: 测试 Resolve（无需认证）

```
POST http://localhost:8089/api/v1/ai/resolve
Content-Type: application/json

{
  "capability": "segmentation",
  "prefer": ["test_provider"]
}
```

## 默认管理员账号

根据 `V2__seed.sql`:
- **Email**: `admin@platform.com`
- **Password**: `admin123`

## 故障排查

### 问题 1: 403 Forbidden

**原因**: 未提供有效的认证 token

**解决方案**:
1. 先调用 `/api/v1/auth/login` 获取 token
2. 在请求头中添加 `Authorization: Bearer {token}`

### 问题 2: 401 Unauthorized

**原因**: Token 已过期或无效

**解决方案**:
1. 重新登录获取新 token
2. 检查 token 格式是否正确

### 问题 3: 登录失败

**检查**:
- 数据库是否已初始化（执行了 V2__seed.sql）
- 密码是否正确（admin123）
- 用户状态是否为 ACTIVE

## 测试脚本说明

### quick_test_admin_api_with_auth.ps1
- 自动登录获取 token
- 使用 token 调用所有 Admin API
- 测试 Resolve 立即生效
- 验证 API Key 不回显明文

### quick_test_admin_api.ps1
- 不使用认证（如果权限已临时开放）
- 快速测试所有功能

## 推荐测试流程

1. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

2. **运行测试脚本**
   ```powershell
   .\quick_test_admin_api_with_auth.ps1
   ```

3. **查看结果**
   - 所有步骤应显示 "PASS"
   - Resolve 应能立即使用新创建的数据
