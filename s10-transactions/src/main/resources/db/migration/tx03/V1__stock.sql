-- Lekcja Tx03ProxyPitfalls: stany magazynowe produktów sklepu (quantity = ile sztuk leży w magazynie).
-- CHECK (quantity >= 0) — baza sama pilnuje, żeby stan nie spadł poniżej zera.
create table stock_items (
    sku      varchar(20)  not null primary key,
    name     varchar(100) not null,
    quantity int          not null check (quantity >= 0)
);

insert into stock_items (sku, name, quantity) values ('SPO-001', 'Kawa ziarnista 1kg', 120);
insert into stock_items (sku, name, quantity) values ('SPO-002', 'Czekolada gorzka', 300);
insert into stock_items (sku, name, quantity) values ('SPO-003', 'Oliwa z oliwek', 0);
