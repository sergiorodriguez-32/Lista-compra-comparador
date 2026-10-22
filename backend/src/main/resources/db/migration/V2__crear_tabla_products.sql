CREATE TABLE products
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source         VARCHAR(20)    NOT NULL DEFAULT 'MANUAL',
    external_id    VARCHAR(100),
    owner_id       BIGINT         REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(200)   NOT NULL,
    brand          VARCHAR(100),
    description    TEXT,
    price          NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    package_amount NUMERIC(10, 3) NOT NULL CHECK (package_amount > 0),
    package_unit   VARCHAR(10)    NOT NULL
        CHECK (package_unit IN ('UNIDAD', 'KG', 'LITRO', 'GRAMO', 'ML')),
    category       VARCHAR(20)    NOT NULL
        CHECK (category IN ('SECO', 'CONGELADO', 'FRESCO', 'FRUTA', 'DROGUERIA')),
    ingredients    TEXT
);

CREATE INDEX idx_products_owner ON products (owner_id);