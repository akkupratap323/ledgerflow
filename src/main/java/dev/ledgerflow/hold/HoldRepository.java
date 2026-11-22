package dev.ledgerflow.hold;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldRepository extends JpaRepository<Hold, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hold h where h.id = :id")
    Optional<Hold> findByIdForUpdate(UUID id);

    @Query("select h.id from Hold h where h.status = dev.ledgerflow.hold.HoldStatus.PENDING and h.expiresAt < :now")
    List<UUID> findExpiredIds(Instant now);

    List<Hold> findByAccountIdAndStatus(UUID accountId, HoldStatus status);
}
