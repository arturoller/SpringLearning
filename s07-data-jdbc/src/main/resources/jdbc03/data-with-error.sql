-- Skrypt z BŁĘDEM w środku: drugi INSERT łamie klucz główny (id = 1 już istnieje po pierwszym).
INSERT INTO product (id, name, category, price, stock) VALUES (1, 'Mysz', 'komputery', 59.00, 30);
INSERT INTO product (id, name, category, price, stock) VALUES (1, 'Duplikat', 'komputery', 1.00, 1);
INSERT INTO product (id, name, category, price, stock) VALUES (2, 'Kubek', 'dom', 24.50, 100);
