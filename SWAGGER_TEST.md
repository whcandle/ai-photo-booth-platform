# Swagger/OpenAPI 测试指南

## 任务 1：Swagger 集成完成 ✅

### 已完成的修改

1. **添加依赖** (`pom.xml`)
   - 添加了 `springdoc-openapi-starter-webmvc-ui` 2.6.0 版本

2. **放行 Swagger 路由** (`SecurityConfig.java`)
   - 放行了 `/swagger-ui.html`、`/swagger-ui/**`、`/v3/api-docs/**`

3. **创建 Swagger 配置** (`OpenApiConfig.java`)
   - 配置了 API 文档基本信息
   - 配置了 JWT Bearer Token 认证支持

---

## 测试方法

### 1. 启动应用

```bash
cd ai-photo-booth-platform
mvn spring-boot:run
```

或者使用 IDE 运行 `PlatformApplication.java`

### 2. 验证 Swagger UI

在浏览器中访问：

```
http://localhost:8080/swagger-ui/index.html
```

**预期结果**：
- ✅ 页面正常加载，显示 Swagger UI 界面
- ✅ 可以看到所有 API 端点（Auth、Admin、Merchant、Device）
- ✅ 每个端点都有详细的参数说明和响应示例

### 3. 验证 OpenAPI JSON

在浏览器中访问：

```
http://localhost:8080/v3/api-docs
```

或者访问完整格式：

```
http://localhost:8080/v3/api-docs.yaml
```

**预期结果**：
- ✅ 返回 JSON/YAML 格式的 OpenAPI 规范文档
- ✅ 包含所有 API 端点的定义

### 4. 测试 JWT 认证（在 Swagger UI 中）

1. 打开 Swagger UI：`http://localhost:8080/swagger-ui/index.html`

2. 找到右上角的 **"Authorize"** 按钮（🔒图标）

3. 点击 "Authorize" 按钮

4. 在弹出窗口中：
   - 输入从 `/api/v1/auth/login` 获取的 JWT Token
   - 格式：直接粘贴 token，不需要加 "Bearer " 前缀
   - 点击 "Authorize" 按钮

5. 测试需要认证的 API：
   - 选择 `/api/v1/admin/templates` (GET)
   - 点击 "Try it out"
   - 点击 "Execute"
   - ✅ 应该返回 200 状态码和模板列表

### 5. 使用 curl 测试（可选）

#### 测试 Swagger UI 访问
```bash
curl -I http://localhost:8080/swagger-ui/index.html
```

**预期结果**：返回 `200 OK`

#### 测试 OpenAPI JSON
```bash
curl http://localhost:8080/v3/api-docs
```

**预期结果**：返回 JSON 格式的 API 文档

---

## 验证清单

- [ ] 应用启动成功，无错误
- [ ] `http://localhost:8080/swagger-ui/index.html` 可以访问
- [ ] `http://localhost:8080/v3/api-docs` 返回 JSON 文档
- [ ] Swagger UI 中可以看到所有 API 端点
- [ ] 可以在 Swagger UI 中使用 JWT Token 测试 API

---

## 常见问题

### Q1: 访问 Swagger UI 返回 404

**原因**：可能是路径不对，SpringDoc 3.x 的路径是 `/swagger-ui/index.html`

**解决**：确保访问的是 `http://localhost:8080/swagger-ui/index.html`（不是 `/swagger-ui.html`）

### Q2: 访问 Swagger UI 返回 401/403

**原因**：SecurityConfig 没有正确放行 Swagger 路由

**解决**：检查 `SecurityConfig.java` 中的 `requestMatchers` 是否包含：
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

### Q3: Swagger UI 中看不到 API 端点

**原因**：可能是 Controller 没有正确扫描

**解决**：确保所有 Controller 都在 `com.mg.platform.web` 包下，并且有 `@RestController` 注解

---

## 下一步

完成 Swagger 测试后，继续完成：

**任务 2：RBAC 权限控制**
- `/api/v1/admin/**` 只允许 ADMIN 角色访问
- `/api/v1/merchant/**` 允许 ADMIN 和 MERCHANT_OWNER 角色访问
