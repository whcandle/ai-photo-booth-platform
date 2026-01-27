-- Flyway: V3__indexes.sql
-- Secondary indexes / unique constraints / views
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- merchants
ALTER TABLE merchants
  ADD UNIQUE KEY uk_merchants_code (code),
  ADD KEY idx_merchants_status (status),
  ADD KEY idx_merchants_created_at (created_at);

-- users
ALTER TABLE users
  ADD UNIQUE KEY uk_users_email (email),
  ADD KEY idx_users_merchant_role (merchant_id, role),
  ADD KEY idx_users_status (status);

-- membership_plans
ALTER TABLE membership_plans
  ADD UNIQUE KEY uk_membership_plans_code (code),
  ADD KEY idx_membership_plans_status (status);

-- merchant_subscriptions
ALTER TABLE merchant_subscriptions
  ADD KEY idx_subs_merchant_status (merchant_id, status),
  ADD KEY idx_subs_end_at (end_at);

-- payments
ALTER TABLE payments
  ADD UNIQUE KEY uk_payments_provider_txn (provider, provider_txn_id),
  ADD KEY idx_payments_merchant (merchant_id, created_at),
  ADD KEY idx_payments_status (status);

-- points_ledger
ALTER TABLE points_ledger
  ADD KEY idx_points_merchant_created (merchant_id, created_at),
  ADD KEY idx_points_ref (ref_type, ref_id);

-- templates
ALTER TABLE templates
  ADD UNIQUE KEY uk_templates_code (code),
  ADD KEY idx_templates_status (status),
  ADD KEY idx_templates_type (type),
  ADD KEY idx_templates_created_at (created_at);

-- template_versions
ALTER TABLE template_versions
  ADD KEY idx_template_versions_template (template_id, status),
  ADD KEY idx_template_versions_version (version);

-- activities
ALTER TABLE activities
  ADD KEY idx_activities_merchant_status (merchant_id, status),
  ADD KEY idx_activities_time (start_at, end_at);

-- activity_templates
ALTER TABLE activity_templates
  ADD UNIQUE KEY uk_activity_template (activity_id, template_id),
  ADD KEY idx_activity_templates_activity (activity_id, sort_order);

-- devices
ALTER TABLE devices
  ADD UNIQUE KEY uk_devices_merchant_code (merchant_id, device_code),
  ADD KEY idx_devices_merchant_status (merchant_id, status),
  ADD KEY idx_devices_last_seen (last_seen_at);

-- device_activity_assignments
ALTER TABLE device_activity_assignments
  ADD KEY idx_daa_device_status (device_id, status),
  ADD KEY idx_daa_activity (activity_id);

-- media_assets
ALTER TABLE media_assets
  ADD KEY idx_media_merchant_created (merchant_id, created_at),
  ADD KEY idx_media_device (device_id),
  ADD KEY idx_media_activity (activity_id),
  ADD KEY idx_media_template (template_id);

-- ai_jobs
ALTER TABLE ai_jobs
  ADD KEY idx_jobs_merchant_status (merchant_id, status, created_at),
  ADD KEY idx_jobs_device (device_id),
  ADD KEY idx_jobs_activity (activity_id),
  ADD KEY idx_jobs_template (template_id);

-- work_orders
ALTER TABLE work_orders
  ADD KEY idx_wo_merchant_status (merchant_id, status),
  ADD KEY idx_wo_status_updated (status, updated_at);

-- model_providers
ALTER TABLE model_providers
  ADD UNIQUE KEY uk_model_providers_code (code),
  ADD KEY idx_model_providers_status (status);

-- provider_api_keys
ALTER TABLE provider_api_keys
  ADD KEY idx_api_keys_provider_status (provider_id, status);

-- provider_usage_records
ALTER TABLE provider_usage_records
  ADD KEY idx_usage_provider_created (provider_id, created_at),
  ADD KEY idx_usage_api_key (api_key_id),
  ADD KEY idx_usage_job (job_id);

-- pricing_rules
ALTER TABLE pricing_rules
  ADD KEY idx_pricing_scope (scope, merchant_id, item_type, effective_from);

-- View: points balance
CREATE OR REPLACE VIEW v_merchant_points_balance AS
SELECT
  merchant_id,
  COALESCE(SUM(delta), 0) AS balance
FROM points_ledger
GROUP BY merchant_id;
