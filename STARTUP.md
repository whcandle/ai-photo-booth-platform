# 启动指南

## 1. 数据库准备

确保 MySQL 已启动，然后创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS ai_photo_booth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 2. 生成密码哈希（重要）

在首次启动前，需要生成正确的密码哈希：

1. 运行 `PasswordGenerator.java` 的 main 方法
2. 复制输出的哈希值
3. 更新 `V2__seed.sql` 中的 `password_hash` 字段

或者直接使用以下命令生成：

```bash
# 在项目根目录执行
mvn exec:java -Dexec.mainClass="com.mg.platform.util.PasswordGenerator"
```

## 3. 启动后端

```bash
cd ai-photo-booth-platform
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

## 4. 启动前端

```bash
cd ai-photo-booth-web-console
npm install
npm run dev
```

前端将在 `http://localhost:3000` 启动

## 5. 测试登录

使用以下账号登录：

- 管理员：`admin@platform.com` / `admin123`
- 商家：`owner@test.com` / `merchant123`

## 6. 创建测试数据流程

1. 使用管理员账号登录，进入 `/admin/templates` 创建模板
2. 为模板添加版本（设置 packageUrl）
3. 使用商家账号登录，进入 `/merchant/activities` 创建活动
4. 在活动详情页绑定模板和设备
5. 设备端可以通过 `/api/v1/device/{deviceId}/activities` 获取活动列表
