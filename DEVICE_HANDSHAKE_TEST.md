# Device Handshake API 测试指南

## API 端点

**POST** `/api/v1/device/handshake`

## 请求格式

```json
{
  "deviceCode": "DV001",
  "secret": "your-secret-key"
}
```

## 响应格式

成功响应：
```json
{
  "success": true,
  "data": {
    "deviceId": 1,
    "deviceToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "serverTime": "2026-01-31T12:00:00+08:00"
  },
  "message": null
}
```

失败响应：
```json
{
  "success": false,
  "data": null,
  "message": "Device not found"
}
```

## Curl 测试示例

### 1. 成功场景测试

首先，确保数据库中有一个设备记录。可以通过以下 SQL 插入测试数据：

```sql
-- 假设 merchant_id = 1 存在
INSERT INTO devices (merchant_id, device_code, secret, name, status)
VALUES (1, 'DV001', 'test-secret-123', 'Test Device', 'ACTIVE');
```

然后执行 curl 测试：

**Linux/Mac:**
```bash
curl -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV001","secret":"test-secret-123"}'
```

**Windows (cmd/PowerShell):**
```cmd
curl -X POST http://localhost:8080/api/v1/device/handshake -H "Content-Type: application/json" -d "{\"deviceCode\":\"DV001\",\"secret\":\"test-secret-123\"}"
```

或者使用 `--data-raw` 参数（推荐）：
```cmd
curl -X POST http://localhost:8080/api/v1/device/handshake -H "Content-Type: application/json" --data-raw "{\"deviceCode\":\"DV001\",\"secret\":\"test-secret-123\"}"
```

**注意**：Windows 下必须使用双引号包裹 JSON，并且内部的双引号需要用 `\"` 转义。

预期响应：
- `success: true`
- `data.deviceId`: 设备ID
- `data.deviceToken`: JWT token 字符串
- `data.expiresIn`: 86400 (24小时，单位：秒)
- `data.serverTime`: 服务器当前时间（ISO 8601格式）

### 2. 失败场景测试

#### 2.1 设备不存在

```bash
curl -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV999","secret":"any-secret"}'
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Device not found"
}
```

#### 2.2 Secret 不匹配

```bash
curl -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV001","secret":"wrong-secret"}'
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Invalid secret"
}
```

#### 2.3 设备状态非 ACTIVE

首先更新设备状态：
```sql
UPDATE devices SET status = 'INACTIVE' WHERE device_code = 'DV001';
```

然后测试：
```bash
curl -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV001","secret":"test-secret-123"}'
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Device is not active"
}
```

## JWT Token 验证

获取到的 `deviceToken` 是一个 JWT，包含以下 claims：

- `sub`: `device:{deviceId}` (subject)
- `deviceId`: 设备ID
- `merchantId`: 商家ID
- `type`: `"device"`

可以使用 [jwt.io](https://jwt.io) 解码查看 token 内容。

## 注意事项

1. **数据库迁移**：确保已运行 Flyway 迁移 `V5__add_device_secret.sql`，为 devices 表添加了 `secret` 字段。

2. **Secret 管理**：
   - 生产环境应使用强随机密钥
   - Secret 应安全存储，不要硬编码在客户端
   - 建议定期轮换密钥

3. **Token 有效期**：默认 24 小时（86400 秒），可在 `application.yml` 中配置 `jwt.expiration`。

4. **时区**：`serverTime` 使用 `Asia/Shanghai` 时区，格式为 ISO 8601。

## 后续步骤

完成 handshake 后，设备可以使用返回的 `deviceToken` 访问其他 Device API。

---

# Device Activities API 测试指南

## API 端点

**GET** `/api/v1/device/{deviceId}/activities`

## 认证要求

此端点需要设备 JWT token 认证。在请求头中添加：
```
Authorization: Bearer <deviceToken>
```

## 请求格式

无需请求体，deviceId 通过路径参数传递。

## 响应格式

成功响应：
```json
{
  "success": true,
  "data": [
    {
      "activityId": 3,
      "name": "店内常驻",
      "status": "ACTIVE",
      "startAt": null,
      "endAt": null
    }
  ],
  "message": null
}
```

失败响应：
```json
{
  "success": false,
  "data": null,
  "message": "Missing or invalid Authorization header"
}
```

或

```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

## Curl 测试示例

### 前置条件

1. 完成 handshake 获取 deviceToken（参考上面的 handshake 测试）
2. 确保设备已绑定到活动：

```sql
-- 假设设备ID为1，活动ID为3
INSERT INTO activities (merchant_id, name, status, start_at, end_at)
VALUES (1, '店内常驻', 'ACTIVE', NULL, NULL);

INSERT INTO device_activity_assignments (device_id, activity_id, status)
VALUES (1, 3, 'ACTIVE');
```

### 1. 成功场景测试

```bash
# 首先获取 deviceToken（假设 deviceId=1）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV001","secret":"test-secret-123"}' \
  | grep -o '"deviceToken":"[^"]*"' | cut -d'"' -f4)

# 使用 token 获取活动列表
curl http://localhost:8080/api/v1/device/1/activities \
  -H "Authorization: Bearer $TOKEN"
```

或者直接使用 token：

```bash
curl http://localhost:8080/api/v1/device/1/activities \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

预期响应：
- `success: true`
- `data`: 活动数组，每个活动包含：
  - `activityId`: 活动ID
  - `name`: 活动名称
  - `status`: 活动状态（ACTIVE）
  - `startAt`: 开始时间（可为 null）
  - `endAt`: 结束时间（可为 null）

### 2. 失败场景测试

#### 2.1 缺少 Authorization 头

```bash
curl http://localhost:8080/api/v1/device/1/activities
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Missing or invalid Authorization header"
}
```

#### 2.2 Token 无效或过期

```bash
curl http://localhost:8080/api/v1/device/1/activities \
  -H "Authorization: Bearer invalid-token"
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

#### 2.3 Token 中的 deviceId 与路径参数不匹配

```bash
# 使用 deviceId=1 的 token 访问 deviceId=2 的活动
curl http://localhost:8080/api/v1/device/2/activities \
  -H "Authorization: Bearer <deviceId=1的token>"
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

#### 2.4 使用用户 token（非设备 token）

```bash
# 使用用户登录获取的 token（type != "device"）
curl http://localhost:8080/api/v1/device/1/activities \
  -H "Authorization: Bearer <user-token>"
```

预期响应：
```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

## 活动过滤规则

API 只返回满足以下条件的活动：

1. **设备绑定**：设备必须通过 `device_activity_assignments` 表绑定到活动，且 `status = 'ACTIVE'`
2. **活动状态**：`activity.status = 'ACTIVE'`
3. **时间范围**：
   - 如果 `startAt` 有值，当前时间必须 >= `startAt`
   - 如果 `endAt` 有值，当前时间必须 <= `endAt`
   - 如果 `startAt` 和 `endAt` 都为 null，则活动始终可用

## 安全说明

1. **Token 验证**：端点会验证：
   - Token 格式正确且未过期
   - Token 类型为 "device"
   - Token 中的 `deviceId` 必须与路径参数 `{deviceId}` 完全匹配

2. **权限隔离**：设备只能访问自己绑定的活动，无法访问其他设备的活动。

3. **Token 有效期**：Token 默认有效期为 24 小时，过期后需要重新 handshake。

---

# Device Templates API 测试指南

## API 端点

**GET** `/api/v1/device/{deviceId}/activities/{activityId}/templates`

## 认证要求

此端点需要设备 JWT token 认证。在请求头中添加：
```
Authorization: Bearer <deviceToken>
```

## 请求格式

无需请求体，deviceId 和 activityId 通过路径参数传递。

## 响应格式

成功响应：
```json
{
  "success": true,
  "data": [
    {
      "templateId": 10,
      "name": "春节国风",
      "coverUrl": "http://.../cover.png",
      "version": "0.1.0",
      "downloadUrl": "http://.../tpl_001_v0.1.0.zip",
      "checksum": "sha256:....",
      "enabled": true
    }
  ],
  "message": "Success"
}
```

失败响应（401 Unauthorized）：
```json
{
  "success": false,
  "data": null,
  "message": "Missing or invalid Authorization header"
}
```

失败响应（403 Forbidden）：
```json
{
  "success": false,
  "data": null,
  "message": "Device does not have access to this activity"
}
```

## Curl 测试示例

### 前置条件

1. 完成 handshake 获取 deviceToken（参考上面的 handshake 测试）
2. 确保设备已绑定到活动：
```sql
-- 假设设备ID为3，活动ID为3
INSERT INTO device_activity_assignments (device_id, activity_id, status)
VALUES (3, 3, 'ACTIVE');
```

3. 确保活动已绑定模板版本：
```sql
-- 假设模板ID为10，模板版本ID为101
INSERT INTO templates (merchant_id, code, name, status, cover_url)
VALUES (1, 'TPL001', '春节国风', 'ACTIVE', 'http://.../cover.png');

INSERT INTO template_versions (template_id, version, package_url, checksum, status)
VALUES (10, '0.1.0', 'http://.../tpl_001_v0.1.0.zip', 'sha256:....', 'ACTIVE');

INSERT INTO activity_templates (activity_id, template_id, template_version_id, is_enabled, sort_order)
VALUES (3, 10, 101, true, 0);
```

### 1. 成功场景测试

```bash
# 首先获取 deviceToken（假设 deviceId=3）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/device/handshake \
  -H "Content-Type: application/json" \
  -d '{"deviceCode":"DV001","secret":"test-secret-123"}' \
  | grep -o '"deviceToken":"[^"]*"' | cut -d'"' -f4)

# 使用 token 获取模板列表
curl http://localhost:8080/api/v1/device/3/activities/3/templates \
  -H "Authorization: Bearer $TOKEN"
```

或者直接使用 token：

```bash
curl http://localhost:8080/api/v1/device/3/activities/3/templates \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

预期响应：
- HTTP 状态码：200 OK
- `success: true`
- `data`: 模板数组，每个模板包含：
  - `templateId`: 模板ID
  - `name`: 模板名称
  - `coverUrl`: 封面图URL
  - `version`: 版本号（来自 template_versions.version）
  - `downloadUrl`: 下载URL（来自 template_versions.package_url）
  - `checksum`: 校验和（来自 template_versions.checksum）
  - `enabled`: 是否启用（来自 activity_templates.is_enabled）

### 2. 失败场景测试

#### 2.1 缺少 Authorization 头

```bash
curl http://localhost:8080/api/v1/device/3/activities/3/templates
```

预期响应：
- HTTP 状态码：401 Unauthorized
```json
{
  "success": false,
  "data": null,
  "message": "Missing or invalid Authorization header"
}
```

#### 2.2 Token 无效或过期

```bash
curl http://localhost:8080/api/v1/device/3/activities/3/templates \
  -H "Authorization: Bearer invalid-token"
```

预期响应：
- HTTP 状态码：401 Unauthorized
```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

#### 2.3 Token 中的 deviceId 与路径参数不匹配

```bash
# 使用 deviceId=1 的 token 访问 deviceId=3 的模板
curl http://localhost:8080/api/v1/device/3/activities/3/templates \
  -H "Authorization: Bearer <deviceId=1的token>"
```

预期响应：
- HTTP 状态码：401 Unauthorized
```json
{
  "success": false,
  "data": null,
  "message": "Invalid or unauthorized device token"
}
```

#### 2.4 设备未绑定到活动（403 Forbidden）

```bash
# 假设设备3没有绑定到活动3
curl http://localhost:8080/api/v1/device/3/activities/999/templates \
  -H "Authorization: Bearer <deviceId=3的token>"
```

预期响应：
- HTTP 状态码：403 Forbidden
```json
{
  "success": false,
  "data": null,
  "message": "Device does not have access to this activity"
}
```

## 模板过滤规则

API 返回满足以下条件的模板：

1. **设备绑定验证**：设备必须通过 `device_activity_assignments` 表绑定到活动，且 `status = 'ACTIVE'`
2. **模板状态**：`templates.status = 'ACTIVE'`
3. **返回所有模板**：返回活动绑定的所有模板（不管 `activity_templates.is_enabled` 的值），但会返回 `enabled` 字段供客户端判断

## 字段说明

- **templateId**: 来自 `templates.id`
- **name**: 来自 `templates.name`
- **coverUrl**: 来自 `templates.cover_url`
- **version**: 来自 `template_versions.version`
- **downloadUrl**: 来自 `template_versions.package_url`
- **checksum**: 来自 `template_versions.checksum`
- **enabled**: 来自 `activity_templates.is_enabled`（布尔值，表示该模板在该活动中是否启用）

## 安全说明

1. **Token 验证**：端点会验证：
   - Token 格式正确且未过期
   - Token 类型为 "device"
   - Token 中的 `deviceId` 必须与路径参数 `{deviceId}` 完全匹配

2. **权限验证**：设备必须绑定到指定的活动，否则返回 403 Forbidden。

3. **权限隔离**：设备只能访问自己绑定的活动的模板，无法访问其他设备的活动模板。

4. **Token 有效期**：Token 默认有效期为 24 小时，过期后需要重新 handshake。
