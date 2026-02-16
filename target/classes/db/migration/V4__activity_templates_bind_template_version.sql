-- Flyway: V4__activity_templates_bind_template_version.sql
-- activity_templates 绑定 template_versions

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 1) 如果存在 (activity_id, template_id) 唯一键，先 DROP（用 information_schema 判断是否存在）
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'activity_templates'
    AND index_name = 'uk_activity_template'
);

SET @drop_stmt := IF(
  @idx_exists > 0,
  'ALTER TABLE activity_templates DROP INDEX uk_activity_template',
  'SELECT 1'
);

PREPARE stmt FROM @drop_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 新增 template_version_id 列（先允许 NULL，用于回填）
ALTER TABLE activity_templates
  ADD COLUMN template_version_id BIGINT UNSIGNED NULL AFTER template_id;

-- 3) 用 template_id 回填 template_version_id
-- 选同 template_id 下 id 最大的 template_versions 作为 latest
UPDATE activity_templates atpl
JOIN (
  SELECT template_id, MAX(id) AS latest_version_id
  FROM template_versions
  GROUP BY template_id
) tv ON tv.template_id = atpl.template_id
SET atpl.template_version_id = tv.latest_version_id;

-- 4) 将 template_version_id 改为 NOT NULL
ALTER TABLE activity_templates
  MODIFY COLUMN template_version_id BIGINT UNSIGNED NOT NULL;

-- 5) 添加外键约束：template_version_id -> template_versions.id
ALTER TABLE activity_templates
  ADD CONSTRAINT fk_at_template_version
    FOREIGN KEY (template_version_id)
    REFERENCES template_versions(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- 6) 添加唯一约束：同一 activity 下同一 template_version 只能出现一次
ALTER TABLE activity_templates
  ADD UNIQUE KEY uk_activity_template_version (activity_id, template_version_id);

-- 7) 添加索引：按活动、按模板版本查询
ALTER TABLE activity_templates
  ADD KEY idx_at_activity (activity_id, sort_order),
  ADD KEY idx_at_template_version (template_version_id);

