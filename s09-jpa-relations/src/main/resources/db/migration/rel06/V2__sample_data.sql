-- Dane startowe (nazwy jak w calym kursie). Ceny zakupu i notatki sa przykladowe.

insert into products (sku, name, price, purchase_price) values ('ELE-001', 'Laptop Pro 14', 5499.99, 4100.00);
insert into products (sku, name, price, purchase_price) values ('SPO-001', 'Kawa ziarnista 1kg', 64.99, 38.50);

insert into customers (name, city, email, internal_note) values ('Jan Kowalski', 'Warszawa', 'jan@example.com', 'VIP - rabat negocjowany indywidualnie');
insert into customers (name, city, email, internal_note) values ('Maria Nowak', 'Kraków', null, null);

insert into orders (number, status, customer_id) values ('ZAM-001', 'NOWE', (select id from customers where name = 'Jan Kowalski'));
insert into orders (number, status, customer_id) values ('ZAM-002', 'OPLACONE', (select id from customers where name = 'Maria Nowak'));
insert into orders (number, status, customer_id) values ('ZAM-003', 'WYSLANE', (select id from customers where name = 'Jan Kowalski'));
