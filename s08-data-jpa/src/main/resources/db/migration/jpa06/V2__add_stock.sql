-- V2: nowa kolumna stanu magazynowego. Zmiana schematu = NOWA migracja, nigdy edycja V1.
alter table products add column stock integer not null default 0;
