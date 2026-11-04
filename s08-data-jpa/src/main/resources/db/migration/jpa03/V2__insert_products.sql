-- Dane startowe lekcji Jpa03QueryMethods: dziesięć produktów (id 1–10) w trzech kategoriach. Kategoria GARDEN
-- celowo nie ma żadnego produktu, a trzy produkty nie mają dostawcy (supplier = NULL).
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Klawiatura',      'COMPUTERS',  149.90,  12, 'Nowak',    TIMESTAMP WITH TIME ZONE '2026-09-01 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Mysz',            'COMPUTERS',   59.00,  30, 'Nowak',    TIMESTAMP WITH TIME ZONE '2026-09-02 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Monitor',         'COMPUTERS',  899.99,   5, 'Kowalski', TIMESTAMP WITH TIME ZONE '2026-09-03 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Laptop',          'COMPUTERS', 3499.00,   3, 'Kowalski', TIMESTAMP WITH TIME ZONE '2026-09-04 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Kubek',           'HOME',        24.50, 100, NULL,       TIMESTAMP WITH TIME ZONE '2026-09-05 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Lampka',          'HOME',        79.00,   0, NULL,       TIMESTAMP WITH TIME ZONE '2026-09-06 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Kubek termiczny', 'HOME',        59.00,  20, 'Nowak',    TIMESTAMP WITH TIME ZONE '2026-09-07 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Zeszyt',          'OFFICE',       4.50, 200, NULL,       TIMESTAMP WITH TIME ZONE '2026-09-08 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Długopis',        'OFFICE',       2.99, 500, 'Kowalski', TIMESTAMP WITH TIME ZONE '2026-09-09 10:00:00+00');
INSERT INTO product (name, category, price, stock, supplier, created_at) VALUES ('Krzesło biurowe', 'OFFICE',     599.00,   0, 'Nowak',    TIMESTAMP WITH TIME ZONE '2026-09-10 10:00:00+00');
