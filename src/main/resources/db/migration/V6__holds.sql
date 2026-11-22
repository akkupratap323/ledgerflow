-- Holds reserve funds (card authorizations, pending payouts) without moving them yet.
ALTER TABLE accounts ADD COLUMN held_minor BIGINT NOT NULL DEFAULT 0;

CREATE TABLE holds (
    id               UUID         PRIMARY KEY,
    account_id       UUID         NOT NULL REFERENCES accounts (id),
    amount_minor     BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    status           VARCHAR(10)  NOT NULL,
    reference        VARCHAR(100),
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at      TIMESTAMP WITH TIME ZONE,
    capture_entry_id UUID REFERENCES journal_entries (id),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_holds_amount CHECK (amount_minor > 0),
    CONSTRAINT ck_holds_status CHECK (status IN ('PENDING', 'CAPTURED', 'VOIDED', 'EXPIRED'))
);

CREATE INDEX ix_holds_status_expires ON holds (status, expires_at);
CREATE INDEX ix_holds_account ON holds (account_id);
