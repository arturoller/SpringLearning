-- Dane startowe. Hasla sa TESTOWE (login-test-123, np. anna-test-123) - tu tylko ich skroty BCrypt.
-- "Teraz" w aplikacji: 2026-10-19 (zegar demonstracyjny z application-cap03.properties).

insert into library_users (username, password_hash, full_name, role)
values ('anna', '{bcrypt}$2a$10$FCIEJUuTdrIfAlzRWbkXeeuhYWE804p5CdgJ4kb0jkWfnky0nbVOi', 'Anna Nowak', 'BIBLIOTEKARZ');
insert into library_users (username, password_hash, full_name, role)
values ('jan', '{bcrypt}$2a$10$kzAtenLZ9YyysA9oUs0MmuQ6q7BIh9Dyf8tBbhCu.dzwCk1NvTFK2', 'Jan Kowalski', 'CZYTELNIK');
insert into library_users (username, password_hash, full_name, role)
values ('maria', '{bcrypt}$2a$10$vmaEXdQl9aYMz0mmk3G1KucnQMOsGm872yc.moczgG2Mw7IN2uE9W', 'Maria Nowak', 'CZYTELNIK');
insert into library_users (username, password_hash, full_name, role)
values ('adam', '{bcrypt}$2a$10$jz8i2fD4SV78xw26.5/8H.ynjtFYcGgTTjsVaE8sr6kgr7.LtaCl2', 'Adam Mazur', 'CZYTELNIK');

insert into books (call_number, title, author, on_loan, version) values ('KSI-001', 'Czysty kod', 'Robert C. Martin', true, 0);
insert into books (call_number, title, author, on_loan, version) values ('KSI-002', 'Java. Podstawy', 'Cay S. Horstmann', true, 0);
insert into books (call_number, title, author, on_loan, version) values ('KSI-003', 'Wzorce projektowe', 'Erich Gamma i in.', true, 0);
insert into books (call_number, title, author, on_loan, version) values ('KSI-004', 'Pragmatyczny programista', 'Andrew Hunt, David Thomas', false, 0);
insert into books (call_number, title, author, on_loan, version) values ('KSI-005', 'Refaktoryzacja', 'Martin Fowler', false, 0);
insert into books (call_number, title, author, on_loan, version) values ('KSI-006', 'Domain-Driven Design', 'Eric Evans', false, 0);

-- 1: Jan, Czysty kod - termin 15.10 minal (4 dni po terminie)
insert into loans (book_id, reader_id, loaned_on, due_on, returned_on, fine, extended)
values (1, 2, date '2026-10-01', date '2026-10-15', null, null, false);
-- 2: Jan, Java. Podstawy - w terminie (do 24.10)
insert into loans (book_id, reader_id, loaned_on, due_on, returned_on, fine, extended)
values (2, 2, date '2026-10-10', date '2026-10-24', null, null, false);
-- 3: Maria, Wzorce projektowe - termin 15.09 minal (34 dni po terminie)
insert into loans (book_id, reader_id, loaned_on, due_on, returned_on, fine, extended)
values (3, 3, date '2026-09-01', date '2026-09-15', null, null, false);
-- 4: Adam, Pragmatyczny programista - zwrocona 5 dni po terminie, kara 2.50 (historia)
insert into loans (book_id, reader_id, loaned_on, due_on, returned_on, fine, extended)
values (4, 4, date '2026-08-01', date '2026-08-15', date '2026-08-20', 2.50, false);
