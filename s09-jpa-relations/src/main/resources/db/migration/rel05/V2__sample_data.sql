-- Dane startowe: 7 klientow sklepu, 6 zamowien od 5 roznych klientow (Jan ma dwa), razem 10 pozycji.
-- Marek Krol i Ewa Lis nie maja zamowien.

insert into customers (name, city) values ('Jan Kowalski', 'Warszawa');
insert into customers (name, city) values ('Maria Nowak', 'Kraków');
insert into customers (name, city) values ('Adam Mazur', 'Gdańsk');
insert into customers (name, city) values ('Zofia Krawczyk', 'Warszawa');
insert into customers (name, city) values ('Ola Pawlak', 'Poznań');
insert into customers (name, city) values ('Marek Król', 'Kraków');
insert into customers (name, city) values ('Ewa Lis', 'Wrocław');

insert into orders (number, status, customer_id) values ('ZAM-001', 'NOWE', (select id from customers where name = 'Jan Kowalski'));
insert into orders (number, status, customer_id) values ('ZAM-002', 'OPLACONE', (select id from customers where name = 'Maria Nowak'));
insert into orders (number, status, customer_id) values ('ZAM-003', 'WYSLANE', (select id from customers where name = 'Adam Mazur'));
insert into orders (number, status, customer_id) values ('ZAM-004', 'DOSTARCZONE', (select id from customers where name = 'Zofia Krawczyk'));
insert into orders (number, status, customer_id) values ('ZAM-005', 'NOWE', (select id from customers where name = 'Jan Kowalski'));
insert into orders (number, status, customer_id) values ('ZAM-006', 'ANULOWANE', (select id from customers where name = 'Ola Pawlak'));

insert into order_lines (product_name, quantity, order_id) values ('Laptop Pro 14', 1, (select id from orders where number = 'ZAM-001'));
insert into order_lines (product_name, quantity, order_id) values ('Słuchawki BT', 2, (select id from orders where number = 'ZAM-001'));
insert into order_lines (product_name, quantity, order_id) values ('Kawa ziarnista 1kg', 3, (select id from orders where number = 'ZAM-002'));
insert into order_lines (product_name, quantity, order_id) values ('Czysty kod', 1, (select id from orders where number = 'ZAM-003'));
insert into order_lines (product_name, quantity, order_id) values ('Java. Podstawy', 1, (select id from orders where number = 'ZAM-003'));
insert into order_lines (product_name, quantity, order_id) values ('Wzorce projektowe', 1, (select id from orders where number = 'ZAM-003'));
insert into order_lines (product_name, quantity, order_id) values ('Kurtka zimowa', 1, (select id from orders where number = 'ZAM-004'));
insert into order_lines (product_name, quantity, order_id) values ('Monitor 27 cali', 2, (select id from orders where number = 'ZAM-005'));
insert into order_lines (product_name, quantity, order_id) values ('Lampka biurkowa', 1, (select id from orders where number = 'ZAM-005'));
insert into order_lines (product_name, quantity, order_id) values ('Czekolada gorzka', 5, (select id from orders where number = 'ZAM-006'));
