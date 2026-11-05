-- Schemat lekcji Jdbc01JdbcClient (sklep). Uruchamia go Spring Boot (spring.sql.init.schema-locations).
CREATE TABLE product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    price            DECIMAL(10,2) NOT NULL,
    stock            INT           NOT NULL,
    discount_percent INT           NULL
);
