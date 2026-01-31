-- Flyway: V5__add_device_secret.sql
-- Add secret field to devices table for device authentication

ALTER TABLE devices
ADD COLUMN secret VARCHAR(255) NULL AFTER device_code;

-- Update existing devices with a default secret (in production, these should be set properly)
-- For testing purposes, we'll set a default secret
UPDATE devices SET secret = CONCAT('default_secret_', id) WHERE secret IS NULL;

-- Make secret NOT NULL after setting defaults (optional, or keep it nullable for flexibility)
-- ALTER TABLE devices MODIFY COLUMN secret VARCHAR(255) NOT NULL;
