package dev.ledgerflow.hold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Releases funds for holds that were never captured or voided before their expiry. */
@Component
@ConditionalOnProperty(name = "ledgerflow.holds.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class HoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryJob.class);

    private final HoldRepository holds;
    private final HoldService service;

    public HoldExpiryJob(HoldRepository holds, HoldService service) {
        this.holds = holds;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${ledgerflow.holds.expiry.interval:PT30S}")
    public void run() {
        expireDue(Instant.now());
    }

    /** Each hold is expired in its own transaction so one failure does not block the rest. */
    public int expireDue(Instant now) {
        int expired = 0;
        for (UUID id : holds.findExpiredIds(now)) {
            try {
                service.expire(id);
                expired++;
            } catch (RuntimeException e) {
                // Most likely captured or voided between the query and the lock.
                log.debug("Skipping hold {}: {}", id, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} holds", expired);
        }
        return expired;
    }
}
