package dev.ledgerflow.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Records an event inside the caller's transaction. */
@Component
public class OutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        try {
            repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, mapper.writeValueAsString(payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize " + eventType, e);
        }
    }
}
