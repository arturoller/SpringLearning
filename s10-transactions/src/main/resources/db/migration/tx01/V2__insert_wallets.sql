-- Dane startowe lekcji Tx01Transactional: portfel 1 Anna Nowak 500.00, portfel 2 Jan Kowalski 100.00.
-- Testy przywracają ten stan przed każdym testem (Tx01TestData.reset).
INSERT INTO wallet (id, owner, balance) VALUES (1, 'Anna Nowak', 500.00);
INSERT INTO wallet (id, owner, balance) VALUES (2, 'Jan Kowalski', 100.00);
