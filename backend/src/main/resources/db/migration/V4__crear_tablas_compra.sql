CREATE TABLE shopping_lists
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shopping_lists_owner ON shopping_lists (owner_id);

-- Solo una lista por defecto por usuario
CREATE UNIQUE INDEX uq_shopping_lists_default
    ON shopping_lists (owner_id) WHERE is_default;

CREATE TABLE shopping_list_items
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    list_id    BIGINT  NOT NULL REFERENCES shopping_lists (id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    packages   INTEGER NOT NULL DEFAULT 1 CHECK (packages > 0),
    checked    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_list_product UNIQUE (list_id, product_id)
);

CREATE INDEX idx_shopping_items_list ON shopping_list_items (list_id);