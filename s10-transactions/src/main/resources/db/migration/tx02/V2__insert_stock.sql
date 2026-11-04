-- Dane startowe lekcji Tx02Propagation: na magazynie 5 klawiatur i 2 myszy; brak zamówień i wpisów audytu.
-- Testy przywracają ten stan przed każdym testem (Tx02TestData.reset).
INSERT INTO stock (product, quantity) VALUES ('Klawiatura', 5);
INSERT INTO stock (product, quantity) VALUES ('Mysz', 2);
