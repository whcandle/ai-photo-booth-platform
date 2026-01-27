-- Flyway: V1__core.sql
-- Core schema (tables + PK/FK)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 0) merchants
CREATE TABLE merchants (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name             VARCHAR(128) NOT NULL,
  code             VARCHAR(64)  NOT NULL,
  status           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
  contact_name     VARCHAR(64)  NULL,
  contact_phone    VARCHAR(32)  NULL,
  contact_email    VARCHAR(128) NULL,
  country          VARCHAR(64)  NULL,
  timezone         VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1) users
CREATE TABLE users (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NULL, -- platform admin can be NULL
  email            VARCHAR(128) NOT NULL,
  phone            VARCHAR(32)  NULL,
  password_hash    VARCHAR(255) NOT NULL,
  display_name     VARCHAR(64)  NULL,
  role             VARCHAR(32)  NOT NULL, -- ADMIN / MERCHANT_OWNER / MERCHANT_STAFF
  status           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
  last_login_at    TIMESTAMP NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_users_merchant
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) membership_plans
CREATE TABLE membership_plans (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code             VARCHAR(64) NOT NULL, -- START/PRO/BUSINESS
  name             VARCHAR(128) NOT NULL,
  billing_period   VARCHAR(16) NOT NULL DEFAULT 'YEAR',
  price_amount     DECIMAL(12,2) NOT NULL,
  price_currency   VARCHAR(8) NOT NULL DEFAULT 'USD',
  included_credits INT UNSIGNED NOT NULL DEFAULT 0,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) merchant_subscriptions
CREATE TABLE merchant_subscriptions (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  plan_id          BIGINT UNSIGNED NOT NULL,
  status           VARCHAR(32) NOT NULL, -- ACTIVE/CANCELED/EXPIRED/TRIALING
  start_at         TIMESTAMP NOT NULL,
  end_at           TIMESTAMP NOT NULL,
  auto_renew       TINYINT(1) NOT NULL DEFAULT 0,
  trial_end_at     TIMESTAMP NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_subs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_subs_plan FOREIGN KEY (plan_id) REFERENCES membership_plans(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) payments
CREATE TABLE payments (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  type             VARCHAR(32) NOT NULL,       -- MEMBERSHIP / CREDIT_TOPUP
  provider         VARCHAR(32) NOT NULL,       -- STRIPE/PAYPAL/...
  provider_txn_id  VARCHAR(128) NULL,
  amount           DECIMAL(12,2) NOT NULL,
  currency         VARCHAR(8) NOT NULL DEFAULT 'USD',
  status           VARCHAR(32) NOT NULL,       -- PENDING/PAID/FAILED/REFUNDED
  paid_at          TIMESTAMP NULL,
  meta_json        JSON NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_payments_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) points_ledger
CREATE TABLE points_ledger (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  type             VARCHAR(32) NOT NULL,      -- CREDIT_GRANT / CREDIT_PURCHASE / CONSUME_IMAGE / CONSUME_VIDEO / ADJUST
  delta            INT NOT NULL,
  ref_type         VARCHAR(32) NULL,
  ref_id           BIGINT UNSIGNED NULL,
  note             VARCHAR(255) NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_points_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_points_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) templates (platform templates)
CREATE TABLE templates (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code             VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NOT NULL,
  description      TEXT NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  type             VARCHAR(32) NOT NULL DEFAULT 'IMAGE', -- IMAGE/VIDEO
  model_provider   VARCHAR(64) NULL,
  model_name       VARCHAR(128) NULL,
  params_schema_json JSON NULL,
  content_json     JSON NOT NULL,
  cover_url        VARCHAR(512) NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_templates_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7) template_versions
CREATE TABLE template_versions (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id      BIGINT UNSIGNED NOT NULL,
  version          VARCHAR(32) NOT NULL,
  package_url      VARCHAR(512) NOT NULL,
  checksum         VARCHAR(128) NULL,
  manifest_json    JSON NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/ROLLBACK
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_template_versions_template FOREIGN KEY (template_id) REFERENCES templates(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8) activities (merchant activities)
CREATE TABLE activities (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(128) NOT NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  start_at         TIMESTAMP NULL,
  end_at           TIMESTAMP NULL,
  description      TEXT NULL,
  settings_json    JSON NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_activities_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_activities_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9) activity_templates
CREATE TABLE activity_templates (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  activity_id      BIGINT UNSIGNED NOT NULL,
  template_id      BIGINT UNSIGNED NOT NULL,
  sort_order       INT NOT NULL DEFAULT 0,
  is_enabled       TINYINT(1) NOT NULL DEFAULT 1,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_at_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_at_template FOREIGN KEY (template_id) REFERENCES templates(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10) devices
CREATE TABLE devices (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  device_code      VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_seen_at     TIMESTAMP NULL,
  client_version   VARCHAR(32) NULL,
  meta_json        JSON NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_devices_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11) device_activity_assignments
CREATE TABLE device_activity_assignments (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id        BIGINT UNSIGNED NOT NULL,
  activity_id      BIGINT UNSIGNED NOT NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE
  activated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deactivated_at   TIMESTAMP NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_daa_device FOREIGN KEY (device_id) REFERENCES devices(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_daa_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_daa_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12) media_assets
CREATE TABLE media_assets (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  device_id        BIGINT UNSIGNED NULL,
  activity_id      BIGINT UNSIGNED NULL,
  template_id      BIGINT UNSIGNED NULL,
  type             VARCHAR(16) NOT NULL, -- PHOTO/VIDEO
  origin           VARCHAR(16) NOT NULL, -- RAW/AI_RESULT/PRINT
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'S3',
  storage_bucket   VARCHAR(128) NULL,
  storage_key      VARCHAR(512) NOT NULL,
  mime_type        VARCHAR(64) NULL,
  size_bytes       BIGINT UNSIGNED NULL,
  width            INT NULL,
  height           INT NULL,
  duration_ms      INT NULL,
  checksum         VARCHAR(128) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_media_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_media_device FOREIGN KEY (device_id) REFERENCES devices(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_media_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_media_template FOREIGN KEY (template_id) REFERENCES templates(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13) ai_jobs
CREATE TABLE ai_jobs (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  device_id        BIGINT UNSIGNED NULL,
  activity_id      BIGINT UNSIGNED NULL,
  template_id      BIGINT UNSIGNED NOT NULL,
  template_version VARCHAR(32) NULL,
  status           VARCHAR(32) NOT NULL, -- QUEUED/RUNNING/SUCCEEDED/FAILED
  input_media_id   BIGINT UNSIGNED NULL,
  output_media_id  BIGINT UNSIGNED NULL,
  input_raw_url    VARCHAR(512) NULL,
  output_urls_json JSON NULL,
  cost_credits     INT NOT NULL DEFAULT 0,
  error_message    VARCHAR(1024) NULL,
  request_json     JSON NULL,
  response_json    JSON NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  finished_at      TIMESTAMP NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_jobs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_jobs_device FOREIGN KEY (device_id) REFERENCES devices(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_jobs_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_jobs_template FOREIGN KEY (template_id) REFERENCES templates(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_jobs_input_media FOREIGN KEY (input_media_id) REFERENCES media_assets(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_jobs_output_media FOREIGN KEY (output_media_id) REFERENCES media_assets(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14) work_orders
CREATE TABLE work_orders (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  type             VARCHAR(32) NOT NULL,
  status           VARCHAR(32) NOT NULL, -- OPEN/IN_PROGRESS/RESOLVED/CLOSED
  title            VARCHAR(128) NOT NULL,
  description      TEXT NULL,
  attachments_json JSON NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  assignee_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_wo_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_wo_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_wo_assignee FOREIGN KEY (assignee_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15) model providers + api keys + usage
CREATE TABLE model_providers (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code             VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NOT NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE provider_api_keys (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_id      BIGINT UNSIGNED NOT NULL,
  name             VARCHAR(128) NOT NULL,
  api_key_cipher   TEXT NOT NULL,
  status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_api_keys_provider FOREIGN KEY (provider_id) REFERENCES model_providers(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE provider_usage_records (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_id      BIGINT UNSIGNED NOT NULL,
  api_key_id       BIGINT UNSIGNED NULL,
  job_id           BIGINT UNSIGNED NULL,
  model_name       VARCHAR(128) NULL,
  input_tokens     INT UNSIGNED NOT NULL DEFAULT 0,
  output_tokens    INT UNSIGNED NOT NULL DEFAULT 0,
  cost_amount      DECIMAL(12,6) NULL,
  cost_currency    VARCHAR(8) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_usage_provider FOREIGN KEY (provider_id) REFERENCES model_providers(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_usage_api_key FOREIGN KEY (api_key_id) REFERENCES provider_api_keys(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_usage_job FOREIGN KEY (job_id) REFERENCES ai_jobs(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16) pricing_rules
CREATE TABLE pricing_rules (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  scope            VARCHAR(16) NOT NULL DEFAULT 'GLOBAL', -- GLOBAL / MERCHANT
  merchant_id      BIGINT UNSIGNED NULL,
  item_type        VARCHAR(16) NOT NULL, -- IMAGE/VIDEO
  credits_per_item INT UNSIGNED NOT NULL,
  effective_from   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  effective_to     TIMESTAMP NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_pricing_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_pricing_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
