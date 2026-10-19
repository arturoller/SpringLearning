-- Lekcja Test04DataJpaTest, sekcja 4: skrypt dla @Sql — „wyprzedaż”: dwa produkty, które sklep właśnie przyjął
-- i od razu przecenił. Wykonuje się w transakcji testu, więc po teście znika razem z nią.
INSERT INTO product (sku, name, category, price, stock) VALUES
    ('DOM-003', 'Odkurzacz',      'DOM',    599.00, 3),
    ('ODZ-003', 'Czapka zimowa',  'ODZIEZ',  39.99, 40);
