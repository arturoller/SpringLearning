-- Lekcja Evt03AsyncAndOutbox: zamówienia oraz tabela outbox (skrzynka nadawcza) z wiadomościami do wysłania.
CREATE TABLE orders (
    id       VARCHAR(20)   NOT NULL PRIMARY KEY,
    customer VARCHAR(100)  NOT NULL,
    total    DECIMAL(10,2) NOT NULL
);

CREATE TABLE outbox (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(20)   NOT NULL,
    type     VARCHAR(50)   NOT NULL,
    payload  VARCHAR(500)  NOT NULL,
    sent     BOOLEAN       NOT NULL DEFAULT FALSE,
    attempts INT           NOT NULL DEFAULT 0
);
