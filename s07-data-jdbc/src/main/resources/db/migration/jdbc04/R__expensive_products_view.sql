-- Migracja POWTARZALNA (R__): bez wersji. Flyway wykonuje ją po wszystkich wersjonowanych i ponawia za każdym razem,
-- gdy zmieni się jej treść (suma kontrolna). Dobra na widoki, procedury i funkcje — rzeczy, które nadpisujemy w całości.
CREATE OR REPLACE VIEW expensive_product AS
    SELECT id, sku, name, price FROM product WHERE price >= 1000;
