-- Lekcja Test06Testcontainers: 7 klientów sklepu (ci sami co w całym kursie). Maria Nowak i Ola Pawlak nie mają e-maila.
INSERT INTO customer (name, city, email, vip) VALUES
    ('Jan Kowalski',   'Warszawa', 'jan@example.com',        TRUE),
    ('Maria Nowak',    'Kraków',   NULL,                     FALSE),
    ('Adam Mazur',     'Gdańsk',   'adam.mazur@example.com', FALSE),
    ('Zofia Krawczyk', 'Warszawa', 'zofia@example.com',      TRUE),
    ('Ola Pawlak',     'Poznań',   NULL,                     FALSE),
    ('Marek Król',     'Kraków',   'marek@example.com',      FALSE),
    ('Ewa Lis',        'Wrocław',  'ewa.lis@example.com',    FALSE);
