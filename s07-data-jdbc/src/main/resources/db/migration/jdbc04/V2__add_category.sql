-- Migracja V2: nowa kolumna. Istniejacych wierszy NIE gubimy - dostaja wartosc domyslna.
ALTER TABLE products ADD COLUMN category VARCHAR(50) DEFAULT 'inne' NOT NULL;
