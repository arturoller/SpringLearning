-- Lekcja Jdbc01JdbcClient: jedna tabela sklepu.
CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price_cents INT          NOT NULL,
    stock       INT          NOT NULL
);
