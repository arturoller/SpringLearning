-- Dane na jeden test (@Sql): dwa produkty. Wykonuja sie w transakcji testu, wiec po tescie znikaja (rollback).
INSERT INTO products (name, price_cents) VALUES ('Kawa', 2999);
INSERT INTO products (name, price_cents) VALUES ('Herbata', 1599);
