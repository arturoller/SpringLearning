-- Schemat lekcji Jdbc05Repository. Ograniczenia (constraints) bazy to ostatnia linia obrony danych:
-- każde ich złamanie Spring zamienia na wyjątek z hierarchii DataAccessException.
CREATE TABLE category (
    code VARCHAR(50) PRIMARY KEY
);

CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    stock    INT           NOT NULL,
    version  BIGINT        DEFAULT 0 NOT NULL, -- numer wersji wiersza: optymistyczne blokowanie (sekcja 5)
    CONSTRAINT uk_product_name UNIQUE (name),                              -- UNIQUE: nazwa produktu jest jedyna
    CONSTRAINT fk_product_category FOREIGN KEY (category) REFERENCES category (code), -- FK: kategoria musi istnieć
    CONSTRAINT ck_product_stock CHECK (stock >= 0)                         -- CHECK: magazyn nie może zejść poniżej zera
);
