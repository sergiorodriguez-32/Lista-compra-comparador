CREATE TABLE users
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email               VARCHAR(255)   NOT NULL UNIQUE,
    password_hash       VARCHAR(255)   NOT NULL,
    display_name        VARCHAR(100)   NOT NULL,
    low_stock_threshold NUMERIC(10, 2) NOT NULL DEFAULT 1 CHECK (low_stock_threshold >= 0)
);
