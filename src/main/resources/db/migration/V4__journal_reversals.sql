-- A reversal is a new entry that mirrors the original. Each entry can be reversed once.
ALTER TABLE journal_entries ADD COLUMN reversal_of UUID REFERENCES journal_entries (id);
ALTER TABLE journal_entries ADD CONSTRAINT uq_journal_entries_reversal_of UNIQUE (reversal_of);
