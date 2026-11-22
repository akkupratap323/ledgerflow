CREATE TABLE accounts (
    id             UUID         PRIMARY KEY,
    code           VARCHAR(100) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    currency       VARCHAR(3)   NOT NULL,
    balance_minor  BIGINT       NOT NULL DEFAULT 0,
    allow_negative BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_accounts_code UNIQUE (code),
    CONSTRAINT ck_accounts_type CHECK (type IN ('ASSET', 'EXPENSE', 'LIABILITY', 'EQUITY', 'REVENUE'))
);
