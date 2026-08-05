CREATE TABLE price_items (
    item_id     VARCHAR(64) PRIMARY KEY,
    item_type   VARCHAR(32) NOT NULL,
    amount      DECIMAL(10,2) NOT NULL
);

INSERT INTO price_items (item_id, item_type, amount) VALUES
    ('size-s', 'SIZE', 0.00),
    ('size-m', 'SIZE', 2.00),
    ('size-l', 'SIZE', 4.00),
    ('dough-classic', 'DOUGH', 0.00),
    ('dough-gluten-free', 'DOUGH', 2.50),
    ('topping-cheese', 'INGREDIENT', 1.50),
    ('topping-pineapple', 'INGREDIENT', 1.00);
