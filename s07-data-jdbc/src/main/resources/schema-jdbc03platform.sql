-- Skrypt wybierany WYŁĄCZNIE przez spring.sql.init.platform=jdbc03platform (lekcja Jdbc03SqlInit, sekcja 1).
-- Boot szuka schema-${platform}.sql w KORZENIU classpath, dlatego ten jeden plik leży tu, a nie w jdbc03/.
-- Inne lekcje go nie wczytają: dla domyślnej platformy "all" Boot szuka tylko schema.sql i schema-all.sql.
CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    stock    INT           NOT NULL
);
