# Entity Mapping 测试指南

## 测试步骤

### 1. 编译测试
```bash
cd D:\workspace\ai-photo-booth-platform
mvn clean compile -DskipTests
```
**预期结果**: `BUILD SUCCESS`

### 2. 运行测试
```bash
mvn test
# 或
mvn -DskipTests=false test
```
**预期结果**: `BUILD SUCCESS`

### 3. 启动应用测试 Entity 映射

#### 前置条件
- MySQL 8.0 已启动
- 数据库 `ai_photo_booth` 已创建
- 数据库连接配置正确（`application.yml`）

#### 启动应用
```bash
mvn spring-boot:run
```

#### 检查 Entity 映射错误
启动时关注以下日志：

**✅ 正常启动标志**:
- 看到 `Started PlatformApplication` 日志
- 没有 Hibernate 相关的错误
- Flyway 迁移成功执行（包括 V6）

**❌ Entity 映射错误标志**:
- `org.hibernate.MappingException` 相关错误
- `org.hibernate.AnnotationException` 相关错误
- `Unable to build Hibernate SessionFactory` 错误
- 表/列/索引不匹配的错误

#### 常见错误及解决方案

1. **表不存在错误**
   - 确保 Flyway 迁移已执行
   - 检查 `flyway_schema_history` 表确认 V6 迁移已应用

2. **列名不匹配**
   - 检查 Entity 中的 `@Column(name = "...")` 是否与数据库列名一致
   - 检查 snake_case 和 camelCase 转换

3. **索引/唯一约束错误**
   - 检查 `@Index` 和 `@UniqueConstraint` 的 columnList 格式
   - 确保列名使用数据库列名（不是 Java 属性名）

4. **外键关联错误**
   - 检查 `@ManyToOne` 和 `@JoinColumn` 配置
   - 确保关联的 Entity 存在且正确映射

### 4. 快速验证命令

```bash
# 完整测试流程
cd D:\workspace\ai-photo-booth-platform
mvn clean compile test
mvn spring-boot:run
```

### 5. 验证 Entity 是否正确加载

启动成功后，可以通过以下方式验证：

1. **检查日志**: 查看是否有 Entity 扫描相关的日志
2. **访问健康检查端点**（如果有）: `http://localhost:8089/actuator/health`
3. **测试 Repository**: 可以编写简单的测试来验证 Repository 是否正常工作

### 6. 如果遇到问题

1. **查看完整错误日志**: 启动时的完整堆栈跟踪
2. **检查数据库**: 确认表结构是否与 Entity 定义一致
   ```sql
   DESCRIBE provider_capabilities;
   DESCRIBE capability_routing_policies;
   SHOW INDEX FROM provider_capabilities;
   SHOW INDEX FROM capability_routing_policies;
   ```
3. **验证 Flyway 迁移**: 
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```

## 当前状态

✅ **编译测试**: 通过
✅ **Maven 测试**: 通过  
⏳ **启动测试**: 需要手动执行（需要数据库连接）

## 已创建的 Entity

1. `ProviderCapability` - 映射 `provider_capabilities` 表
2. `CapabilityRoutingPolicy` - 映射 `capability_routing_policies` 表

## 已创建的 Repository

1. `ProviderCapabilityRepository`
2. `CapabilityRoutingPolicyRepository`
