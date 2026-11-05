-- Schemat lekcji Jdbc03SqlInit. BEZ "IF NOT EXISTS" — celowo (pułapka z sekcji 4 lekcji).
CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    stock    INT           NOT NULL
);
