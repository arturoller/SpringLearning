-- Dane startowe lekcji Jpa05PagingSorting: dwanaście produktów (id 1–12). CELOWO kilka produktów ma tę samą cenę
-- (24.50 × 2, 59.00 × 3) — przy sortowaniu po samej cenie ich kolejność nie jest ustalona (sekcja 3).
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Klawiatura',      'COMPUTERS',  149.90,  12, TIMESTAMP WITH TIME ZONE '2026-09-01 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Mysz',            'COMPUTERS',   59.00,  30, TIMESTAMP WITH TIME ZONE '2026-09-02 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Monitor',         'COMPUTERS',  899.99,   5, TIMESTAMP WITH TIME ZONE '2026-09-03 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Laptop',          'COMPUTERS', 3499.00,   3, TIMESTAMP WITH TIME ZONE '2026-09-04 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Kubek',           'HOME',        24.50, 100, TIMESTAMP WITH TIME ZONE '2026-09-05 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Lampka',          'HOME',        79.00,   0, TIMESTAMP WITH TIME ZONE '2026-09-06 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Kubek termiczny', 'HOME',        59.00,  20, TIMESTAMP WITH TIME ZONE '2026-09-07 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Zeszyt',          'OFFICE',       4.50, 200, TIMESTAMP WITH TIME ZONE '2026-09-08 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Długopis',        'OFFICE',       2.99, 500, TIMESTAMP WITH TIME ZONE '2026-09-09 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Krzesło biurowe', 'OFFICE',     599.00,   0, TIMESTAMP WITH TIME ZONE '2026-09-10 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Podkładka pod mysz', 'COMPUTERS', 24.50, 40, TIMESTAMP WITH TIME ZONE '2026-09-11 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Segregator',      'OFFICE',      59.00,  60, TIMESTAMP WITH TIME ZONE '2026-09-12 10:00:00+00');
