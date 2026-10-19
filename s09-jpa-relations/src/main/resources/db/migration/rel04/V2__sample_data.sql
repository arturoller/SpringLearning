-- Dane startowe sklepu (te same nazwy co w calym kursie). Id nadaje baza, klucze obce szukamy podzapytaniem.

insert into customers (name, city, email) values ('Jan Kowalski', 'Warszawa', 'jan@example.com');
insert into customers (name, city, email) values ('Maria Nowak', 'Kraków', null);
insert into customers (name, city, email) values ('Adam Mazur', 'Gdańsk', 'adam.mazur@example.com');

insert into orders (number, status, customer_id)
values ('ZAM-001', 'NOWE', (select id from customers where name = 'Jan Kowalski'));
insert into orders (number, status, customer_id)
values ('ZAM-002', 'OPLACONE', (select id from customers where name = 'Maria Nowak'));
insert into orders (number, status, customer_id)
values ('ZAM-003', 'WYSLANE', (select id from customers where name = 'Jan Kowalski'));

insert into order_lines (product_name, quantity, order_id)
values ('Laptop Pro 14', 1, (select id from orders where number = 'ZAM-001'));
insert into order_lines (product_name, quantity, order_id)
values ('Słuchawki BT', 2, (select id from orders where number = 'ZAM-001'));
insert into order_lines (product_name, quantity, order_id)
values ('Kawa ziarnista 1kg', 3, (select id from orders where number = 'ZAM-002'));
insert into order_lines (product_name, quantity, order_id)
values ('Czysty kod', 1, (select id from orders where number = 'ZAM-003'));
