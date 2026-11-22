package dev.ledgerflow.journal;

/** Outcome of a post. {@code replayed} is true when an earlier entry with the same key was returned. */
public record PostResult(JournalEntry entry, boolean replayed) {
}
