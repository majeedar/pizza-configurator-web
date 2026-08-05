-- Backs customer registration/login and the order-history dashboard. Orders and
-- pending reviews are linked to the customer who placed them; nullable because
-- pre-existing rows from before this migration have no customer to point to.
CREATE TABLE customers (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    phone_number   VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE orders ADD COLUMN customer_id UUID REFERENCES customers(id);
ALTER TABLE pending_reviews ADD COLUMN customer_id UUID REFERENCES customers(id);
