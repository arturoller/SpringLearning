-- Migracja wersji 2: zmiana istniejącej tabeli (ALTER TABLE) — to właśnie tego nie umie zrobić schema.sql.
-- Kolumna NOT NULL dostaje DEFAULT, żeby dało się ją dodać do tabeli, która już ma wiersze.
ALTER TABLE product ADD COLUMN stock INT DEFAULT 0 NOT NULL;
