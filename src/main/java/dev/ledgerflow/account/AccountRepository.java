package dev.ledgerflow.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByCode(String code);

    boolean existsByCode(String code);

    /** Resolves an id without loading the entity, so a later locking read is not served stale. */
    @Query("select a.id from Account a where a.code = :code")
    Optional<UUID> findIdByCode(String code);

    /** Row lock used while posting so concurrent writers serialize per account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(UUID id);
}
