-- Wariant "bezpieczny": można go uruchamiać wielokrotnie na tej samej, trwałej bazie.
CREATE TABLE IF NOT EXISTS product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    stock    INT           NOT NULL
);
