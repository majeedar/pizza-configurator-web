CREATE TABLE orders (
    order_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_number        VARCHAR(10)  NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    total_price            DECIMAL(10,2) NOT NULL,
    custom_notes           TEXT,
    pickup_security_token  VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMPTZ  DEFAULT now()
);

CREATE TABLE order_items (
    item_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID REFERENCES orders(order_id) ON DELETE CASCADE,
    base_pizza_id   VARCHAR(64)  NOT NULL,
    chosen_size     VARCHAR(5)   NOT NULL,
    chosen_dough    VARCHAR(32)  NOT NULL,
    modifications   JSONB        NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL
);
