-- Dane startowe lekcji Tx04ReadOnlyIsolation: portfel 1 Anna Nowak 100.00, portfel 2 Jan Kowalski 50.00
-- (w obu tabelach; versioned_wallet startuje z wersją 0). Testy przywracają ten stan przed każdym testem
-- (Tx04TestData.reset).
INSERT INTO wallet (id, owner, balance) VALUES (1, 'Anna Nowak', 100.00), (2, 'Jan Kowalski', 50.00);
INSERT INTO versioned_wallet (id, owner, balance, version) VALUES (1, 'Anna Nowak', 100.00, 0), (2, 'Jan Kowalski', 50.00, 0);
