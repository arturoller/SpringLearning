-- Migracja POWTARZALNA (R__<opis>.sql, bez numeru wersji): Flyway wykonuje ją za każdym razem, gdy zmieni się
-- jej treść (suma kontrolna), i zawsze PO wszystkich migracjach wersjonowanych. Idealna na widoki i procedury.
-- CREATE OR REPLACE sprawia, że kolejne wykonanie nie zawiedzie, gdy widok już istnieje.
CREATE OR REPLACE VIEW product_overview AS
SELECT name, category, price, stock, price * stock AS stock_value
FROM product;
