package uk.gov.hmcts.reform.fact.data.api.audit;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.AuditableCourtEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditActionType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Change;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Entity listener implementation for all {@link AuditableCourtEntity} derived entities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditableCourtEntityListener implements ApplicationContextAware {

    private final ObjectMapper objectMapper;

    private ApplicationContext applicationContext;
    private final AtomicReference<EntityManager> entityManagerRef = new AtomicReference<>();

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // Though we have the application context, we don't want to perform
        // lookups at this point as the objects we require are almost certainly
        // still in construction.
    }

    @PrePersist
    public void beforePersist(AuditableCourtEntity entity) {
        writeAudit(entity, AuditActionType.INSERT);
    }

    @PreUpdate
    public void beforeUpdate(AuditableCourtEntity entity) {
        writeAudit(entity, AuditActionType.UPDATE);
    }

    @PreRemove
    public void beforeRemove(AuditableCourtEntity entity) {
        writeAudit(entity, AuditActionType.DELETE);
    }

    private boolean ensureEntityManager() {
        synchronized (entityManagerRef) {
            if (entityManagerRef.get() == null && applicationContext != null) {
                entityManagerRef.set(applicationContext.getBean(EntityManager.class));
            }
        }
        return entityManagerRef.get() != null;
    }

    private void writeAudit(AuditableCourtEntity entity, AuditActionType auditActionType) {
        if (ensureEntityManager()) {
            AuditableCourtEntity previousEntity = auditActionType == AuditActionType.INSERT
                ? null
                : findPreviousEntity(entity);
            writeAuditRecord(entity, previousEntity, auditActionType);
        } else {
            log.error("No entity manager available during an audit operation");
            throw new IllegalStateException("No entity manager available during an audit operation");
        }
    }

    private AuditableCourtEntity findPreviousEntity(final AuditableCourtEntity entity) {

        // Need to run our lookup for the previous entity on a different
        // thread. If we don't, JPA/Hibernate will intercept and return the
        // data that's about to be persisted as part of the event we're
        // listening to.

        AtomicReference<AuditableCourtEntity> previousEntity = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();
        try {
            Thread lookupThread = new Thread(() -> {
                try {
                    previousEntity.set(entityManagerRef.get().find(entity.getClass(), entity.getId()));
                } catch (Exception e) {
                    log.error(
                        "Unexpected error while looking up previous entity during an audit operation: {}",
                        e.getMessage()
                    );
                    exception.set(e);
                }
            });
            lookupThread.start();
            lookupThread.join();
            if (exception.get() != null) {
                throw new IllegalStateException(
                    "Unexpected error while looking up previous entity during an audit operation",
                    exception.get()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return previousEntity.get();
    }

    private final void writeAuditRecord(AuditableCourtEntity entity, AuditableCourtEntity previous,
                                        AuditActionType operationType) {
        Audit.AuditBuilder audit = Audit.builder()
            .courtId(entity.getCourtId())
            .actionType(operationType)
            .actionEntity(entity.getClass().getSimpleName())
            .createdAt(ZonedDateTime.now())
            .userId(getSSOUserId());
        if (operationType != AuditActionType.DELETE) {
            audit.actionDataDiff(generateDiffs(previous, entity));
        }
        entityManagerRef.get().persist(audit.build());
    }

    private static UUID getSSOUserId() {
        // FIXME: this will need to be determined for the given session. Either
        //        via the security context, or some other mechanism yet to be
        //        determined
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private List<Change> generateDiffs(AuditableCourtEntity previous, AuditableCourtEntity current) {
        List<Change> diffs = new ArrayList<>();
        try {
            // convert the entities into maps
            String previousString = previous != null ? objectMapper.writeValueAsString(previous) : "{}";
            String currentString = objectMapper.writeValueAsString(current);
            Map<?, ?> previousMap = objectMapper.readValue(previousString, Map.class);
            Map<?, ?> currentMap = objectMapper.readValue(currentString, Map.class);
            // diff the maps
            currentMap.forEach((key, value) -> {
                if (!previousMap.containsKey(key)) {
                    diffs.add(new Change(key.toString(), null, value));
                } else if (!Objects.equals(previousMap.get(key), value)) {
                    diffs.add(new Change(key.toString(), previousMap.get(key), value));
                }
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to extract diffs for an entity during auditing", e);
        }
        return diffs;
    }
}
