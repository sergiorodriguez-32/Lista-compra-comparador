CREATE TABLE pantry_items
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id   BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity   NUMERIC(10, 3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    CONSTRAINT uq_pantry_owner_product UNIQUE (owner_id, product_id)
);

CREATE INDEX idx_pantry_owner ON pantry_items (owner_id);