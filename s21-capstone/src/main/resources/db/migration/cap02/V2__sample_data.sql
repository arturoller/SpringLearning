-- Dane startowe. "Teraz" w aplikacji to 2026-10-19 10:00 (zegar demonstracyjny z application-cap02.properties).

insert into desks (code, name) values ('SERWIS-1', 'Stanowisko 1 - laptopy i smartfony');
insert into desks (code, name) values ('SERWIS-2', 'Stanowisko 2 - ekspresy do kawy');

-- 1: jutro 14:00-15:00 (mozna jeszcze anulowac - do 2026-10-19 14:00)
insert into reservations (desk_id, customer_email, starts_at, ends_at, status, created_at)
values (1, 'jan@example.com', timestamp '2026-10-20 14:00:00', timestamp '2026-10-20 15:00:00', 'POTWIERDZONA', timestamp '2026-10-15 09:00:00');
-- 2: dzis 15:00-16:00 (za 5 godzin - za pozno na anulowanie)
insert into reservations (desk_id, customer_email, starts_at, ends_at, status, created_at)
values (1, 'zofia@example.com', timestamp '2026-10-19 15:00:00', timestamp '2026-10-19 16:00:00', 'POTWIERDZONA', timestamp '2026-10-16 12:00:00');
-- 3: pojutrze 9:00-11:00 na stanowisku 2
insert into reservations (desk_id, customer_email, starts_at, ends_at, status, created_at)
values (2, 'marek@example.com', timestamp '2026-10-21 09:00:00', timestamp '2026-10-21 11:00:00', 'POTWIERDZONA', timestamp '2026-10-17 18:30:00');
-- 4: wczoraj (historia)
insert into reservations (desk_id, customer_email, starts_at, ends_at, status, created_at)
values (1, 'adam.mazur@example.com', timestamp '2026-10-18 10:00:00', timestamp '2026-10-18 11:00:00', 'POTWIERDZONA', timestamp '2026-10-10 08:00:00');
-- 5: anulowana - nie zajmuje zadnej godziny
insert into reservations (desk_id, customer_email, starts_at, ends_at, status, created_at, cancelled_at)
values (2, 'ewa.lis@example.com', timestamp '2026-10-22 12:00:00', timestamp '2026-10-22 13:00:00', 'ANULOWANA', timestamp '2026-10-12 10:00:00', timestamp '2026-10-13 10:00:00');

insert into occupied_slots (reservation_id, desk_id, slot_start) values (1, 1, timestamp '2026-10-20 14:00:00');
insert into occupied_slots (reservation_id, desk_id, slot_start) values (2, 1, timestamp '2026-10-19 15:00:00');
insert into occupied_slots (reservation_id, desk_id, slot_start) values (3, 2, timestamp '2026-10-21 09:00:00');
insert into occupied_slots (reservation_id, desk_id, slot_start) values (3, 2, timestamp '2026-10-21 10:00:00');
insert into occupied_slots (reservation_id, desk_id, slot_start) values (4, 1, timestamp '2026-10-18 10:00:00');
