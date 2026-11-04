-- Migracja wersji 1 (Flyway): pierwsza wersja tabeli product. Nazwa: V<wersja>__<opis>.sql (DWA podkreślniki po wersji).
-- Po wykonaniu Flyway zapisze w flyway_schema_history wersję, opis i sumę kontrolną (checksum) tego pliku.
-- Tego pliku NIGDY nie edytujemy po tym, jak poszedł na jakąkolwiek bazę — zmiany robimy kolejnymi migracjami.
CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL
);
