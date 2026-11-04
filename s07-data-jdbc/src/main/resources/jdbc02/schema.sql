-- Schemat lekcji Jdbc02JdbcTemplate (sklep: produkty, klienci, zamówienia z pozycjami).
CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    stock    INT           NOT NULL
);

CREATE TABLE customer (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
);

-- Nazwa "orders" (liczba mnoga), bo ORDER jest słowem zastrzeżonym w SQL.
CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer (id),
    order_date  DATE   NOT NULL
);

CREATE TABLE order_line (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders (id),
    product_id BIGINT        NOT NULL REFERENCES product (id),
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL
);
