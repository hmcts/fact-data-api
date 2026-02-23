package uk.gov.hmcts.reform.fact.data.api.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class AuditableCourtEntityListenerTest {

    private static final UUID COURT_ID = UUID.randomUUID();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuditableCourtEntityListener listener;

    @Captor
    ArgumentCaptor<Audit> auditCaptor;

    @BeforeEach
    void setUp() {
        when(applicationContext.getBean(EntityManager.class)).thenReturn(entityManager);
        listener.setApplicationContext(applicationContext);
    }

    @Test
    void shouldNotPersistAuditOnMissingEntityManager() {
        when(applicationContext.getBean(EntityManager.class)).thenReturn(null);
        Court court = createCourt();
        assertThrows(IllegalStateException.class, () -> listener.beforePersist(court));
        verify(entityManager, times(0)).persist(any(Audit.class));
        verify(entityManager, times(0)).find(any(), any());
    }

    @Test
    void shouldNotPersistAuditIfPreviousStateLookupFails() {
        Court court = createCourt();

        when(entityManager.find(Audit.class, court.getId())).thenThrow(new RuntimeException());

        assertThrows(IllegalStateException.class, () -> listener.beforeUpdate(court));
        verify(entityManager, times(0)).persist(any(Audit.class));
        verify(entityManager, times(1)).find(Court.class, court.getId());
    }

    @Test
    void shouldPersistAuditOnBeforePersist() {
        Court court = createCourt();
        listener.beforePersist(court);
        verify(entityManager, times(1)).persist(any(Audit.class));
        verify(entityManager, times(0)).find(any(), any());
    }

    @Test
    void shouldPersistAuditOnBeforeUpdate() {
        Court court = createCourt();
        listener.beforeUpdate(court);
        verify(entityManager, times(1)).persist(any(Audit.class));
        verify(entityManager, times(1)).find(Court.class, court.getId());
    }

    @Test
    void shouldPersistAuditOnBeforeRemove() {
        Court court = createCourt();
        listener.beforeRemove(court);
        verify(entityManager, times(1)).persist(any(Audit.class));
        verify(entityManager, times(1)).find(Court.class, court.getId());
    }

    @Test
    void generateDiffsShouldReturnChangesWhenValuesDiffer() throws Exception {
        String json = objectMapper.writeValueAsString(createCourt());
        Court courtPrevious = objectMapper.readValue(json, Court.class);
        Court courtCurrent = objectMapper.readValue(json, Court.class);
        courtCurrent.setName("Court Name Updated");

        when(entityManager.find(Court.class, courtCurrent.getId())).thenReturn(courtPrevious);
        listener.beforeUpdate(courtCurrent);

        verify(entityManager, times(1)).persist(auditCaptor.capture());
        verify(entityManager, times(1)).find(Court.class, courtCurrent.getId());
        Audit audit = auditCaptor.getValue();
        assertEquals(1, audit.getActionDataDiff().size());
        assertEquals("name", audit.getActionDataDiff().getFirst().field());
    }

    @Test
    void shouldStillWriteAuditIfGenerateDiffFails() throws Exception {
        String json = objectMapper.writeValueAsString(createCourt());
        Court courtPrevious = objectMapper.readValue(json, Court.class);
        Court courtCurrent = objectMapper.readValue(json, Court.class);
        courtCurrent.setName("Court Name Updated");

        when(entityManager.find(Court.class, courtCurrent.getId())).thenReturn(courtPrevious);
        when(objectMapper.writeValueAsString(courtPrevious)).thenThrow(new JsonParseException("test exception"));

        listener.beforeUpdate(courtCurrent);

        verify(entityManager, times(1)).persist(auditCaptor.capture());
        verify(entityManager, times(1)).find(Court.class, courtCurrent.getId());
        Audit audit = auditCaptor.getValue();
        assertEquals(0, audit.getActionDataDiff().size());
    }

    private Court createCourt() {
        return Court.builder()
            .id(COURT_ID)
            .name("Test Court")
            .slug("test-court")
            .open(Boolean.FALSE)
            .regionId(UUID.randomUUID())
            .isServiceCentre(Boolean.FALSE)
            .build();
    }
}
