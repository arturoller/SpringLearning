-- Lekcja Tx01Transactional: konta klientów sklepu (saldo w groszach).
-- CHECK (balance >= 0) — baza sama pilnuje, żeby saldo nie spadło poniżej zera.
create table accounts (
    number  varchar(20)  not null primary key,
    owner   varchar(100) not null,
    balance bigint       not null check (balance >= 0)
);

insert into accounts (number, owner, balance) values ('PL-JAN', 'Jan Kowalski', 100000);
insert into accounts (number, owner, balance) values ('PL-MARIA', 'Maria Nowak', 50000);
