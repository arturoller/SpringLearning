-- Lekcja Tx04ReadOnlyIsolation: produkty sklepu i ich stany magazynowe (stock = liczba sztuk na stanie).
-- CHECK (stock >= 0) — baza sama pilnuje, żeby stan nie spadł poniżej zera.
create table products (
    sku      varchar(20)  not null primary key,
    name     varchar(100) not null,
    category varchar(20)  not null,
    stock    int          not null check (stock >= 0)
);

insert into products (sku, name, category, stock) values ('ELE-001', 'Laptop Pro 14', 'ELEKTRONIKA', 7);
insert into products (sku, name, category, stock) values ('ELE-002', 'Smartfon X', 'ELEKTRONIKA', 0);
insert into products (sku, name, category, stock) values ('ELE-004', 'Monitor 27 cali', 'ELEKTRONIKA', 4);
insert into products (sku, name, category, stock) values ('DOM-001', 'Ekspres do kawy', 'DOM', 2);
insert into products (sku, name, category, stock) values ('DOM-002', 'Lampka biurkowa', 'DOM', 18);

-- Te same produkty, ale z kolumną version — do blokowania optymistycznego (@Version, sekcja 5 lekcji).
create table versioned_products (
    sku     varchar(20)  not null primary key,
    name    varchar(100) not null,
    stock   int          not null check (stock >= 0),
    version bigint       not null
);

insert into versioned_products (sku, name, stock, version) values ('ELE-001', 'Laptop Pro 14', 7, 0);
insert into versioned_products (sku, name, stock, version) values ('DOM-001', 'Ekspres do kawy', 2, 0);
