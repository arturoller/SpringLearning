-- Lekcja Jdbc02JdbcTemplate: klienci sklepu.
CREATE TABLE customers (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL
);
