-- Lekcja Tx03ProxyPitfalls: stany magazynowe (ile sztuk produktu leży w magazynie).
create table stock (
    product  varchar(100) not null primary key,
    quantity int          not null check (quantity >= 0)
);

insert into stock (product, quantity) values ('Kawa', 10);
insert into stock (product, quantity) values ('Herbata', 5);
insert into stock (product, quantity) values ('Czekolada', 0);
