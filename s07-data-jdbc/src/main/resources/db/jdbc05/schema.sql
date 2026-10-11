-- Lekcja Jdbc05Repository: produkty z unikalna nazwa (UNIQUE) i obowiazkowymi polami (NOT NULL).
CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    price_cents INT          NOT NULL
);
