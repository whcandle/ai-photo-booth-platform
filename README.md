# AI Photo Booth Platform

Spring Boot 平台后端服务，提供商家端、管理端和设备端 API。

## 技术栈

- Spring Boot 3.3.2
- Java 17
- MySQL 8
- Flyway (数据库迁移)
- JWT (认证)
- JPA/Hibernate

## 数据库配置

数据库：`ai_photo_booth`
用户名：`root`
密码：`1`

确保 MySQL 已启动，并且数据库已创建（Flyway 会自动创建表结构）。

## 启动步骤

1. 确保 MySQL 已启动并创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS ai_photo_booth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `src/main/resources/application.yml` 中的数据库连接信息（如需要）

3. 启动应用：
```bash
mvn spring-boot:run
```

或者使用 IDE 运行 `PlatformApplication.java`

4. 应用将在 `http://localhost:8080` 启动

## 默认账号

- 管理员：`admin@platform.com` / `admin123`
- 商家：`owner@test.com` / `merchant123`

注意：密码哈希在 V2__seed.sql 中，实际使用时应该用 BCrypt 加密。建议首次启动后通过代码生成正确的密码哈希。

## API 端点

### 认证
- `POST /api/v1/auth/login` - 登录

### 设备端 API
- `POST /api/v1/device/heartbeat` - 设备心跳
- `GET /api/v1/device/{deviceId}/activities` - 获取设备可用活动
- `GET /api/v1/device/{deviceId}/activities/{activityId}/templates` - 获取活动模板

### 商家端 API
- `GET /api/v1/merchant/activities?merchantId={id}` - 获取活动列表
- `POST /api/v1/merchant/activities` - 创建活动
- `POST /api/v1/merchant/activities/{id}/templates` - 绑定模板到活动
- `POST /api/v1/merchant/activities/{id}/devices` - 绑定设备到活动
- `GET /api/v1/merchant/devices?merchantId={id}` - 获取设备列表
- `POST /api/v1/merchant/devices` - 创建设备

### 管理端 API
- `GET /api/v1/admin/templates` - 获取所有模板
- `POST /api/v1/admin/templates` - 创建模板
- `POST /api/v1/admin/templates/{id}/versions` - 创建模板版本

## 开发说明

### 数据库迁移

Flyway 会在应用启动时自动执行 `src/main/resources/db/migration/` 下的 SQL 文件：
- `V1__core.sql` - 核心表结构
- `V2__seed.sql` - 初始数据
- `V3__indexes.sql` - 索引和约束

### 密码加密

当前 seed 数据中的密码哈希是示例值。实际使用时，应该通过以下方式生成：

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("your-password");
```

## 下一步

1. 实现 JWT 过滤器，保护需要认证的 API
2. 实现设备 token 认证
3. 实现 AI Job 创建和查询
4. 实现支付和点数系统
5. 实现文件上传和存储
