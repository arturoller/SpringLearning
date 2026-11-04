-- Dane startowe lekcji Rel01ManyToOne: czterech klientów (id 1–4) i pięć zamówień (id 1–5).
-- Zamówienia należą do TRZECH różnych klientów; Piotr (id 4) celowo nie ma żadnego zamówienia.
INSERT INTO customer (name, email) VALUES ('Anna Nowak',       'anna@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Jan Kowalski',     'jan@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Ewa Wiśniewska',   'ewa@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Piotr Zieliński',  'piotr@sklep.pl');

INSERT INTO orders (order_number, ordered_on, customer_id) VALUES ('ZAM-001', DATE '2026-10-01', 1);
INSERT INTO orders (order_number, ordered_on, customer_id) VALUES ('ZAM-002', DATE '2026-10-02', 2);
INSERT INTO orders (order_number, ordered_on, customer_id) VALUES ('ZAM-003', DATE '2026-10-03', 1);
INSERT INTO orders (order_number, ordered_on, customer_id) VALUES ('ZAM-004', DATE '2026-10-04', 3);
INSERT INTO orders (order_number, ordered_on, customer_id) VALUES ('ZAM-005', DATE '2026-10-05', 2);
