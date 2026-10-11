-- Migracja POWTARZALNA (R__): wykonywana od nowa za kazdym razem, gdy zmieni sie jej tresc (suma kontrolna).
-- Dobra na widoki i procedury - dlatego CREATE OR REPLACE. Biegnie ZAWSZE po wszystkich wersjonowanych (V).
CREATE OR REPLACE VIEW product_names AS
    SELECT name, category FROM products ORDER BY name;
