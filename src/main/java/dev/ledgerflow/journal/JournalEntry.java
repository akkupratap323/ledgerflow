package dev.ledgerflow.journal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** An immutable, balanced set of postings. Entries are never updated or deleted, only reversed. */
@Entity
@Table(name = "journal_entries")
public class JournalEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, updatable = false)
    private String requestHash;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.PERSIST)
    @OrderBy("lineNo")
    private List<Posting> postings = new ArrayList<>();

    /** Ids are assigned in Java, so tell Spring Data to persist instead of merge. */
    @Transient
    private boolean isNew = true;

    protected JournalEntry() {
        // for JPA
    }

    public JournalEntry(String reference, String description, String currency) {
        this.id = UUID.randomUUID();
        this.reference = reference;
        this.description = description;
        this.currency = currency.toUpperCase();
        this.createdAt = Instant.now();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    void bindIdempotencyKey(String key, String hash) {
        this.idempotencyKey = key;
        this.requestHash = hash;
    }

    void addPosting(Posting posting) {
        postings.add(posting);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public List<Posting> getPostings() {
        return Collections.unmodifiableList(postings);
    }
}
