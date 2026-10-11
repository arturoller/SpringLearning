-- Migracja V1: pierwsza wersja tabeli produktow.
-- Nazwa pliku: V + numer wersji + DWA podkreslniki + opis (podkreslniki w opisie zamieniaja sie na spacje).
CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    price_cents INT          NOT NULL
);
