-- Backs the MANUAL_REVIEW workflow (CLAUDE.md §5): a customer comment that ai-parser
-- couldn't confidently map becomes one of these instead of an order. Staff resolve it
-- into a validated structured configuration + price; the customer then confirms that
-- resolution, which is what actually creates the order.
CREATE TABLE pending_reviews (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_pizza_id           VARCHAR(64)  NOT NULL,
    chosen_size             VARCHAR(5)   NOT NULL,
    chosen_dough            VARCHAR(32)  NOT NULL,
    modifications           JSONB        NOT NULL,
    raw_comment             TEXT         NOT NULL,
    phone_number            VARCHAR(32)  NOT NULL,
    status                  VARCHAR(16)  NOT NULL,
    resolved_base_pizza_id  VARCHAR(64),
    resolved_size           VARCHAR(5),
    resolved_dough          VARCHAR(32),
    resolved_modifications  JSONB,
    resolved_total_price    DECIMAL(10,2),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at             TIMESTAMPTZ
);
