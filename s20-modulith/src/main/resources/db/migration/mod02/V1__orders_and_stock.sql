-- Lekcja Mod02ModuleEvents: tabele dwóch modułów. Każdy moduł ma SWOJE tabele i nie czyta tabel innego modułu:
-- orders należy do modułu orders, stock do modułu inventory.
CREATE TABLE orders
(
    order_id VARCHAR(20) PRIMARY KEY,
    sku      VARCHAR(20) NOT NULL,
    quantity INT         NOT NULL CHECK (quantity > 0),
    status   VARCHAR(20) NOT NULL
);

CREATE TABLE stock
(
    sku      VARCHAR(20) PRIMARY KEY,
    quantity INT NOT NULL CHECK (quantity >= 0)
);

INSERT INTO stock (sku, quantity)
VALUES ('ELE-001', 7),
       ('ELE-003', 25),
       ('KSI-001', 15),
       ('SPO-001', 120),
       ('DOM-001', 2);
