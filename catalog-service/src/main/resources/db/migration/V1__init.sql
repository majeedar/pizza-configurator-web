CREATE TABLE pizzas (
    id           VARCHAR(64) PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    description  VARCHAR(256) NOT NULL,
    base_price   DECIMAL(10,2) NOT NULL
);

INSERT INTO pizzas (id, name, description, base_price) VALUES
    ('margherita', 'Margherita', 'Tomato, mozzarella, basil', 8.50),
    ('hawaii', 'Hawaii', 'Tomato, mozzarella, ham, pineapple', 9.50),
    ('salami', 'Salami', 'Tomato, mozzarella, salami', 9.00);
