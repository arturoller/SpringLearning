-- Lekcja Evt02TransactionalEvents: zamówienia i punkty lojalnościowe klientów.
CREATE TABLE orders (
    id       VARCHAR(20)   NOT NULL PRIMARY KEY,
    customer VARCHAR(100)  NOT NULL,
    total    DECIMAL(10,2) NOT NULL
);

CREATE TABLE loyalty_points (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(20)  NOT NULL,
    customer VARCHAR(100) NOT NULL,
    points   INT          NOT NULL
);
