-- Clients send an Idempotency-Key header so a retried request never posts twice.
ALTER TABLE journal_entries ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE journal_entries ADD COLUMN request_hash VARCHAR(64);
ALTER TABLE journal_entries ADD CONSTRAINT uq_journal_entries_idempotency_key UNIQUE (idempotency_key);
