CREATE TABLE pizza_default_ingredients (
    id            BIGSERIAL PRIMARY KEY,
    pizza_id      VARCHAR(64) NOT NULL REFERENCES pizzas(id),
    ingredient_id VARCHAR(64) NOT NULL,
    name          VARCHAR(64) NOT NULL,
    removable     BOOLEAN NOT NULL
);

CREATE TABLE pizza_allowed_extras (
    id            BIGSERIAL PRIMARY KEY,
    pizza_id      VARCHAR(64) NOT NULL REFERENCES pizzas(id),
    ingredient_id VARCHAR(64) NOT NULL,
    name          VARCHAR(64) NOT NULL,
    type          VARCHAR(16) NOT NULL,
    unit          VARCHAR(16) NOT NULL
);

-- Margherita: classic recipe, no pineapple offered as an extra.
INSERT INTO pizza_default_ingredients (pizza_id, ingredient_id, name, removable) VALUES
    ('margherita', 'tomato-sauce', 'Tomato sauce', false),
    ('margherita', 'mozzarella-base', 'Mozzarella', false),
    ('margherita', 'basil', 'Basil', true);

INSERT INTO pizza_allowed_extras (pizza_id, ingredient_id, name, type, unit) VALUES
    ('margherita', 'mozzarella', 'Mozzarella', 'CHEESE', 'PORTION'),
    ('margherita', 'gorgonzola', 'Gorgonzola', 'CHEESE', 'PORTION'),
    ('margherita', 'parmesan', 'Parmesan', 'CHEESE', 'PORTION'),
    ('margherita', 'vegan-cheese', 'Vegan cheese', 'CHEESE', 'PORTION'),
    ('margherita', 'anchovies', 'Anchovies', 'TOPPING', 'PIECE');

-- Hawaii: pineapple is a removable default, not an addable extra — the Hawaii
-- invariant rule (rule-service) already blocks adding pineapple beyond the default
-- quantity, so offering it as a steppable extra would just always fail validation.
INSERT INTO pizza_default_ingredients (pizza_id, ingredient_id, name, removable) VALUES
    ('hawaii', 'tomato-sauce', 'Tomato sauce', false),
    ('hawaii', 'mozzarella-base', 'Mozzarella', false),
    ('hawaii', 'ham', 'Ham', true),
    ('hawaii', 'pineapple', 'Pineapple', true);

INSERT INTO pizza_allowed_extras (pizza_id, ingredient_id, name, type, unit) VALUES
    ('hawaii', 'mozzarella', 'Mozzarella', 'CHEESE', 'PORTION'),
    ('hawaii', 'gorgonzola', 'Gorgonzola', 'CHEESE', 'PORTION'),
    ('hawaii', 'parmesan', 'Parmesan', 'CHEESE', 'PORTION'),
    ('hawaii', 'vegan-cheese', 'Vegan cheese', 'CHEESE', 'PORTION'),
    ('hawaii', 'anchovies', 'Anchovies', 'TOPPING', 'PIECE');

-- Salami: no default pineapple, so it's fine as an extra here.
INSERT INTO pizza_default_ingredients (pizza_id, ingredient_id, name, removable) VALUES
    ('salami', 'tomato-sauce', 'Tomato sauce', false),
    ('salami', 'mozzarella-base', 'Mozzarella', false),
    ('salami', 'salami', 'Salami', true);

INSERT INTO pizza_allowed_extras (pizza_id, ingredient_id, name, type, unit) VALUES
    ('salami', 'mozzarella', 'Mozzarella', 'CHEESE', 'PORTION'),
    ('salami', 'gorgonzola', 'Gorgonzola', 'CHEESE', 'PORTION'),
    ('salami', 'parmesan', 'Parmesan', 'CHEESE', 'PORTION'),
    ('salami', 'vegan-cheese', 'Vegan cheese', 'CHEESE', 'PORTION'),
    ('salami', 'pineapple', 'Pineapple', 'TOPPING', 'PIECE'),
    ('salami', 'anchovies', 'Anchovies', 'TOPPING', 'PIECE');
