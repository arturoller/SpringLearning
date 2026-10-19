-- Migracja V3: zmiana istniejącego schematu — nowa kolumna ze stanem magazynowym.
-- Nie edytujemy V1, tylko DODAJEMY nową migrację: V1 jest już zapisana w historii z sumą kontrolną.
ALTER TABLE product ADD COLUMN stock INT DEFAULT 0 NOT NULL;
UPDATE product SET stock = 7 WHERE sku = 'ELE-001';
UPDATE product SET stock = 25 WHERE sku = 'ELE-003';
