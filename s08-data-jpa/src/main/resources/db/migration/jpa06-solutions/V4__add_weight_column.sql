-- Rozwiązanie ćwiczenia 1 lekcji Jpa06FlywayWithJpa: NOWA migracja (kolejny numer wersji), nigdy edycja starej.
-- Kolumna dopuszcza null: istniejące wiersze nie mają jeszcze wagi.
alter table products add column weight_grams integer;
