-- Each posting records the account balance after it was applied and a gapless
-- per-account sequence number, so statements can be rebuilt in exact order.
ALTER TABLE accounts ADD COLUMN posting_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE postings ADD COLUMN account_seq BIGINT NOT NULL DEFAULT 0;
ALTER TABLE postings ADD COLUMN balance_after_minor BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_postings_account_seq ON postings (account_id, account_seq);
