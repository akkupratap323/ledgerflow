package dev.ledgerflow.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            select e.id from OutboxEvent e
            where e.status = dev.ledgerflow.outbox.OutboxStatus.PENDING and e.nextAttemptAt <= :now
            order by e.createdAt
            """)
    List<UUID> findDueIds(Instant now, Pageable limit);

    List<OutboxEvent> findByAggregateId(UUID aggregateId);
}
