CREATE TABLE journal_entries (
    id          UUID         PRIMARY KEY,
    reference   VARCHAR(100),
    description VARCHAR(500),
    currency    VARCHAR(3)   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE postings (
    id           UUID        PRIMARY KEY,
    entry_id     UUID        NOT NULL REFERENCES journal_entries (id),
    account_id   UUID        NOT NULL REFERENCES accounts (id),
    line_no      INT         NOT NULL,
    side         VARCHAR(6)  NOT NULL,
    amount_minor BIGINT      NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_postings_side CHECK (side IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_postings_amount CHECK (amount_minor > 0),
    CONSTRAINT uq_postings_line UNIQUE (entry_id, line_no)
);

CREATE INDEX ix_postings_account_created ON postings (account_id, created_at);
CREATE INDEX ix_journal_entries_reference ON journal_entries (reference);
