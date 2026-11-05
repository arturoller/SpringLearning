-- V3 lekcji Jpa06FlywayWithJpa: NOWA KOLUMNA = nowa migracja (nigdy edycja V1!) + nowe pole w encji Product.
-- DEFAULT 0 wypełnia istniejące wiersze, dzięki czemu NOT NULL nie łamie danych z V2.
ALTER TABLE product ADD COLUMN weight_grams INTEGER DEFAULT 0 NOT NULL;
-- Dane w migracji: znane wagi uzupełniamy od razu; reszta zostaje z wartością domyślną 0.
UPDATE product SET weight_grams = 450 WHERE name = 'Klawiatura';
UPDATE product SET weight_grams = 90  WHERE name = 'Mysz';
UPDATE product SET weight_grams = 320 WHERE name = 'Kubek';
