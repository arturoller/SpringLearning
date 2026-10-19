-- Wersja 3 schematu: kody rabatowe - przygotowane pod CWICZENIE 1 projektu Cap01ShopApi.
-- Nowa funkcja = NOWA migracja. Nigdy nie zmieniamy migracji, ktora juz trafila do jakiejkolwiek bazy
-- (Flyway pamieta sume kontrolna kazdego pliku i przy zmianie odmowi startu).

create table discount_codes (
    code        varchar(20) primary key,
    percent     integer     not null,
    valid_until date        not null,
    constraint ck_discount_codes_percent check (percent between 1 and 90)
);

insert into discount_codes (code, percent, valid_until) values ('JESIEN10', 10, date '2026-11-30');
insert into discount_codes (code, percent, valid_until) values ('VIP15', 15, date '2027-12-31');
insert into discount_codes (code, percent, valid_until) values ('WIOSNA20', 20, date '2026-05-31');
