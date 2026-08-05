-- admin-service no longer stores prices/rules canonically — pricing-service (price_db)
-- and rule-service (rules_db) each own their own data now, and admin-service's
-- endpoints are thin proxies to them (see PricingServiceClient/RuleServiceClient).
-- admin_db's only job is recording what was changed via the admin API and when.
CREATE TABLE admin_audit_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action_type  VARCHAR(32)  NOT NULL,
    item_id      VARCHAR(64)  NOT NULL,
    old_value    TEXT,
    new_value    TEXT         NOT NULL,
    changed_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
