-- Dane startowe sklepu (te same nazwy i ceny co w calym kursie).
-- Identyfikatory nadaje baza (identity), wiec w nowej bazie produkty maja id 1..14, klienci 1..7, zamowienia 1..5.

insert into products (sku, name, category, price, stock) values ('ELE-001', 'Laptop Pro 14', 'ELEKTRONIKA', 5499.99, 7);
insert into products (sku, name, category, price, stock) values ('ELE-002', 'Smartfon X', 'ELEKTRONIKA', 2999.00, 0);
insert into products (sku, name, category, price, stock) values ('ELE-003', 'Słuchawki BT', 'ELEKTRONIKA', 349.90, 25);
insert into products (sku, name, category, price, stock) values ('ELE-004', 'Monitor 27 cali', 'ELEKTRONIKA', 1299.00, 4);
insert into products (sku, name, category, price, stock) values ('SPO-001', 'Kawa ziarnista 1kg', 'SPOZYWCZE', 64.99, 120);
insert into products (sku, name, category, price, stock) values ('SPO-002', 'Czekolada gorzka', 'SPOZYWCZE', 7.49, 300);
insert into products (sku, name, category, price, stock) values ('SPO-003', 'Oliwa z oliwek', 'SPOZYWCZE', 42.00, 0);
insert into products (sku, name, category, price, stock) values ('KSI-001', 'Czysty kod', 'KSIAZKI', 79.00, 15);
insert into products (sku, name, category, price, stock) values ('KSI-002', 'Java. Podstawy', 'KSIAZKI', 129.00, 9);
insert into products (sku, name, category, price, stock) values ('KSI-003', 'Wzorce projektowe', 'KSIAZKI', 99.00, 3);
insert into products (sku, name, category, price, stock) values ('ODZ-001', 'Kurtka zimowa', 'ODZIEZ', 459.00, 12);
insert into products (sku, name, category, price, stock) values ('ODZ-002', 'T-shirt bawełniany', 'ODZIEZ', 49.99, 80);
insert into products (sku, name, category, price, stock) values ('DOM-001', 'Ekspres do kawy', 'DOM', 1899.00, 2);
insert into products (sku, name, category, price, stock) values ('DOM-002', 'Lampka biurkowa', 'DOM', 129.00, 18);

insert into customers (name, city, email, vip) values ('Jan Kowalski', 'Warszawa', 'jan@example.com', true);
insert into customers (name, city, email, vip) values ('Maria Nowak', 'Kraków', null, false);
insert into customers (name, city, email, vip) values ('Adam Mazur', 'Gdańsk', 'adam.mazur@example.com', false);
insert into customers (name, city, email, vip) values ('Zofia Krawczyk', 'Warszawa', 'zofia@example.com', true);
insert into customers (name, city, email, vip) values ('Ola Pawlak', 'Poznań', null, false);
insert into customers (name, city, email, vip) values ('Marek Król', 'Kraków', 'marek@example.com', false);
insert into customers (name, city, email, vip) values ('Ewa Lis', 'Wrocław', 'ewa.lis@example.com', false);

-- Zamowienia ZAM-001..ZAM-005 (numer = "ZAM-" + id). Suma (total) = suma (cena * ilosc) pozycji.
insert into orders (customer_id, status, total, created_at) values (1, 'NOWE', 764.79, timestamp '2026-10-01 09:15:00');
insert into orders (customer_id, status, total, created_at) values (2, 'OPLACONE', 208.00, timestamp '2026-10-02 12:30:00');
insert into orders (customer_id, status, total, created_at) values (1, 'WYSLANE', 5499.99, timestamp '2026-10-03 18:05:00');
insert into orders (customer_id, status, total, created_at) values (4, 'DOSTARCZONE', 2028.98, timestamp '2026-10-05 08:00:00');
insert into orders (customer_id, status, total, created_at) values (3, 'ANULOWANE', 459.00, timestamp '2026-10-06 21:40:00');

insert into order_lines (order_id, product_id, quantity, unit_price) values (1, 3, 2, 349.90);
insert into order_lines (order_id, product_id, quantity, unit_price) values (1, 5, 1, 64.99);
insert into order_lines (order_id, product_id, quantity, unit_price) values (2, 8, 1, 79.00);
insert into order_lines (order_id, product_id, quantity, unit_price) values (2, 9, 1, 129.00);
insert into order_lines (order_id, product_id, quantity, unit_price) values (3, 1, 1, 5499.99);
insert into order_lines (order_id, product_id, quantity, unit_price) values (4, 13, 1, 1899.00);
insert into order_lines (order_id, product_id, quantity, unit_price) values (4, 5, 2, 64.99);
insert into order_lines (order_id, product_id, quantity, unit_price) values (5, 11, 1, 459.00);
