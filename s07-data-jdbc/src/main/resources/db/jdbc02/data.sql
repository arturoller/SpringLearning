-- Dane startowe lekcji Jdbc02JdbcTemplate: 14 produktów sklepu (ten sam zestaw co w JavaLearning).
INSERT INTO product (sku, name, category, price, stock) VALUES
    ('ELE-001', 'Laptop Pro 14', 'ELEKTRONIKA', 5499.99, 7),
    ('ELE-002', 'Smartfon X', 'ELEKTRONIKA', 2999.00, 0),
    ('ELE-003', 'Słuchawki BT', 'ELEKTRONIKA', 349.90, 25),
    ('ELE-004', 'Monitor 27 cali', 'ELEKTRONIKA', 1299.00, 4),
    ('SPO-001', 'Kawa ziarnista 1kg', 'SPOZYWCZE', 64.99, 120),
    ('SPO-002', 'Czekolada gorzka', 'SPOZYWCZE', 7.49, 300),
    ('SPO-003', 'Oliwa z oliwek', 'SPOZYWCZE', 42.00, 0),
    ('KSI-001', 'Czysty kod', 'KSIAZKI', 79.00, 15),
    ('KSI-002', 'Java. Podstawy', 'KSIAZKI', 129.00, 9),
    ('KSI-003', 'Wzorce projektowe', 'KSIAZKI', 99.00, 3),
    ('ODZ-001', 'Kurtka zimowa', 'ODZIEZ', 459.00, 12),
    ('ODZ-002', 'T-shirt bawełniany', 'ODZIEZ', 49.99, 80),
    ('DOM-001', 'Ekspres do kawy', 'DOM', 1899.00, 2),
    ('DOM-002', 'Lampka biurkowa', 'DOM', 129.00, 18);

-- Klienci (email bywa NULL — Maria Nowak i Ola Pawlak nie podały adresu).
INSERT INTO customer (name, city, email, vip) VALUES
    ('Jan Kowalski', 'Warszawa', 'jan@example.com', TRUE),
    ('Maria Nowak', 'Kraków', NULL, FALSE),
    ('Adam Mazur', 'Gdańsk', 'adam.mazur@example.com', FALSE),
    ('Zofia Krawczyk', 'Warszawa', 'zofia@example.com', TRUE),
    ('Ola Pawlak', 'Poznań', NULL, FALSE),
    ('Marek Król', 'Kraków', 'marek@example.com', FALSE),
    ('Ewa Lis', 'Wrocław', 'ewa.lis@example.com', FALSE);

-- Zamówienia (klucz obcy do klienta).
INSERT INTO shop_order (number, customer_id, status) VALUES
    ('ZAM-001', 1, 'NOWE'),
    ('ZAM-002', 1, 'OPLACONE'),
    ('ZAM-003', 4, 'WYSLANE');
