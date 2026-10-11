-- Lekcja Jdbc03SqlInit: kategorie produktow. Skrypt schematu (struktura) - uruchamiany PRZED data.sql.
CREATE TABLE categories (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
