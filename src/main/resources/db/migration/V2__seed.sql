-- Flyway: V2__seed.sql
-- Seed / initial data
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT INTO membership_plans (code, name, billing_period, price_amount, price_currency, included_credits, status)
VALUES
  ('START', 'Start', 'YEAR', 199.00, 'USD', 200, 'ACTIVE'),
  ('PRO', 'Pro', 'YEAR', 299.00, 'USD', 500, 'ACTIVE'),
  ('BUSINESS', 'Business', 'YEAR', 399.00, 'USD', 700, 'ACTIVE');

INSERT INTO model_providers (code, name, status)
VALUES ('OPENAI', 'OpenAI', 'ACTIVE');

-- 默认计费：图片 1 点，视频 5 点（示例）
INSERT INTO pricing_rules (scope, merchant_id, item_type, credits_per_item, effective_from)
VALUES
  ('GLOBAL', NULL, 'IMAGE', 1, CURRENT_TIMESTAMP),
  ('GLOBAL', NULL, 'VIDEO', 5, CURRENT_TIMESTAMP);

-- 创建测试商家
INSERT INTO merchants (name, code, status, contact_email, timezone)
VALUES ('Test Merchant', 'TEST001', 'ACTIVE', 'merchant@test.com', 'Asia/Shanghai');

-- 创建平台管理员账号（密码: admin123，需要在实际使用时用 BCrypt 加密）
-- 注意：这里使用简单的哈希，实际应该用 BCrypt
INSERT INTO users (merchant_id, email, password_hash, display_name, role, status)
VALUES (NULL, 'admin@platform.com', '$2a$10$nKTtFrTt7C.oXLhq2dqu5OT03qB4Ue58SXbBxsp6vSqdrPypbdhmW', 'Platform Admin', 'ADMIN', 'ACTIVE');

-- 创建商家管理员账号（密码: merchant123）
INSERT INTO users (merchant_id, email, password_hash, display_name, role, status)
VALUES (1, 'owner@test.com', '$2a$10$5udJROskz9o2mtCbb08eE.xSHIBbsIibRIKRkVU8f9BbDjxDUi2aO', 'Merchant Owner', 'MERCHANT_OWNER', 'ACTIVE');
