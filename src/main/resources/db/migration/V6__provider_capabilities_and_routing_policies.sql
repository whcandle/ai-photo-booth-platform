-- Flyway: V6__provider_capabilities_and_routing_policies.sql
-- Create provider_capabilities and capability_routing_policies tables
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- A) provider_capabilities
CREATE TABLE provider_capabilities (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_id          BIGINT UNSIGNED NOT NULL,
  capability           VARCHAR(64) NOT NULL,
  endpoint             VARCHAR(512) NULL,
  status               VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  priority             INT NOT NULL DEFAULT 100,
  default_timeout_ms   INT NOT NULL DEFAULT 8000,
  default_params_json  JSON NULL,
  created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_provider_capability (provider_id, capability),
  KEY idx_pc_capability_status_priority (capability, status, priority),
  CONSTRAINT fk_pc_provider FOREIGN KEY (provider_id) REFERENCES model_providers(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- B) capability_routing_policies
CREATE TABLE capability_routing_policies (
  id                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  scope                     VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',
  merchant_id               BIGINT UNSIGNED NULL,
  capability                VARCHAR(64) NOT NULL,
  prefer_providers_json     JSON NULL,
  retry_count               INT NOT NULL DEFAULT 0,
  failover_on_http_codes_json JSON NULL,
  max_cost_tier             INT NULL,
  status                    VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scope_capability (scope, merchant_id, capability),
  KEY idx_policy_capability_status (capability, status),
  CONSTRAINT fk_policy_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
