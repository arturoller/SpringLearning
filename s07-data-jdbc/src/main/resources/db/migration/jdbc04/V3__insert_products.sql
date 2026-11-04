-- Migracja wersji 3: migracja z DANYMI (nie ze strukturą). Dane referencyjne/startowe też wersjonujemy.
INSERT INTO product (name, category, price, stock) VALUES ('Klawiatura', 'komputery', 149.90, 12);
INSERT INTO product (name, category, price, stock) VALUES ('Mysz', 'komputery', 59.00, 30);
INSERT INTO product (name, category, price, stock) VALUES ('Monitor', 'komputery', 899.99, 5);
INSERT INTO product (name, category, price, stock) VALUES ('Kubek', 'dom', 24.50, 100);
INSERT INTO product (name, category, price, stock) VALUES ('Lampka', 'dom', 79.00, 0);
