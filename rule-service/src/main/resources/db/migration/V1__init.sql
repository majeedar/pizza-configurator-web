CREATE TABLE rule_thresholds (
    rule_id     VARCHAR(64) PRIMARY KEY,
    value       JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO rule_thresholds (rule_id, value) VALUES
    ('cheese-cap', '2'),
    ('gluten-free-sizes', '["S","M"]'),
    ('oven-thermal-limit', '10');
