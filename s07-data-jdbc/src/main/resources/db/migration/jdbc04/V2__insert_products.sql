-- Migracja V2: dane startowe (produkty sklepu).
INSERT INTO product (sku, name, category, price) VALUES
    ('ELE-001', 'Laptop Pro 14', 'ELEKTRONIKA', 5499.99),
    ('ELE-003', 'Słuchawki BT', 'ELEKTRONIKA', 349.90),
    ('SPO-001', 'Kawa ziarnista 1kg', 'SPOZYWCZE', 64.99),
    ('KSI-001', 'Czysty kod', 'KSIAZKI', 79.00),
    ('KSI-002', 'Java. Podstawy', 'KSIAZKI', 129.00),
    ('DOM-002', 'Lampka biurkowa', 'DOM', 129.00);
