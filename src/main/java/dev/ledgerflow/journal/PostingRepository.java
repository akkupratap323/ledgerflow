package dev.ledgerflow.journal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    /** Net debit minus credit per account, straight from the postings. */
    @Query("""
            select p.account.id, sum(case when p.side = dev.ledgerflow.common.Side.DEBIT
                                          then p.amountMinor else -p.amountMinor end), count(p)
            from Posting p group by p.account.id
            """)
    List<Object[]> netDebitsByAccount();

    @Query(value = """
            select p from Posting p join fetch p.entry
            where p.account.id = :accountId and p.createdAt >= :from and p.createdAt < :to
            order by p.accountSeq
            """,
            countQuery = """
            select count(p) from Posting p
            where p.account.id = :accountId and p.createdAt >= :from and p.createdAt < :to
            """)
    Page<Posting> findStatement(UUID accountId, Instant from, Instant to, Pageable pageable);
}
