-- Schemat lekcji Tx04ReadOnlyIsolation: dwa rodzaje portfela klienta.
-- wallet           — bez kolumny wersji (tu pokazujemy lost update i blokadę pesymistyczną),
-- versioned_wallet — z kolumną version (blokowanie optymistyczne, @Version w encji VersionedWallet).
CREATE TABLE wallet (
    id      BIGINT         PRIMARY KEY,
    owner   VARCHAR(100)   NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0)
);

CREATE TABLE versioned_wallet (
    id      BIGINT         PRIMARY KEY,
    owner   VARCHAR(100)   NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    version BIGINT         NOT NULL,
    CONSTRAINT ck_versioned_wallet_balance CHECK (balance >= 0)
);
