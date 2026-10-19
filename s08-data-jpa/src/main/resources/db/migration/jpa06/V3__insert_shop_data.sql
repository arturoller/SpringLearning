-- V3: dane startowe sklepu — te same 14 produktów i 7 klientów co w całym kursie.
insert into products (id, sku, name, category, price, stock) values
    (1,  'ELE-001', 'Laptop Pro 14',      'ELEKTRONIKA', 5499.99,   7),
    (2,  'ELE-002', 'Smartfon X',         'ELEKTRONIKA', 2999.00,   0),
    (3,  'ELE-003', 'Słuchawki BT',       'ELEKTRONIKA',  349.90,  25),
    (4,  'ELE-004', 'Monitor 27 cali',    'ELEKTRONIKA', 1299.00,   4),
    (5,  'SPO-001', 'Kawa ziarnista 1kg', 'SPOZYWCZE',     64.99, 120),
    (6,  'SPO-002', 'Czekolada gorzka',   'SPOZYWCZE',      7.49, 300),
    (7,  'SPO-003', 'Oliwa z oliwek',     'SPOZYWCZE',     42.00,   0),
    (8,  'KSI-001', 'Czysty kod',         'KSIAZKI',       79.00,  15),
    (9,  'KSI-002', 'Java. Podstawy',     'KSIAZKI',      129.00,   9),
    (10, 'KSI-003', 'Wzorce projektowe',  'KSIAZKI',       99.00,   3),
    (11, 'ODZ-001', 'Kurtka zimowa',      'ODZIEZ',       459.00,  12),
    (12, 'ODZ-002', 'T-shirt bawełniany', 'ODZIEZ',        49.99,  80),
    (13, 'DOM-001', 'Ekspres do kawy',    'DOM',         1899.00,   2),
    (14, 'DOM-002', 'Lampka biurkowa',    'DOM',          129.00,  18);

insert into customers (full_name, city, email, vip) values
    ('Jan Kowalski',   'Warszawa', 'jan@example.com',        true),
    ('Maria Nowak',    'Kraków',   null,                     false),
    ('Adam Mazur',     'Gdańsk',   'adam.mazur@example.com', false),
    ('Zofia Krawczyk', 'Warszawa', 'zofia@example.com',      true),
    ('Ola Pawlak',     'Poznań',   null,                     false),
    ('Marek Król',     'Kraków',   'marek@example.com',      false),
    ('Ewa Lis',        'Wrocław',  'ewa.lis@example.com',    false);
