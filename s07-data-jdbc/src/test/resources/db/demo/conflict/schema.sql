-- Skrypt dla spring.sql.init, ktory tworzy TE SAMA tabele co migracja Flyway V1 - konflikt dwoch mechanizmow.
CREATE TABLE products (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL);
