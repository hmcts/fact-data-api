package uk.gov.hmcts.reform.fact.data.api.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LockRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Lock Concurrency")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class LockConcurrencyTest {

    private static final String TEST_COURT_NAME = "Test Lock Concurrency Court";
    private static final int THREAD_COUNT = 20;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    LockRepository lockRepository;

    @Autowired
    LockService lockService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuditUserContext auditUserContext;

    private Court testCourt;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        lockRepository.deleteAll();

        // Audited entities in this test require an active user id in thread-local context.
        auditUserContext.setUserId(UUID.randomUUID());

        Region region = regionRepository.save(
            Region.builder()
                  .name("Test Region " + UUID.randomUUID())
                  .build()
        );
        testCourt = courtRepository.save(Court.builder()
                                              .name(TEST_COURT_NAME)
                                              .open(Boolean.FALSE)
                                              .regionId(region.getId())
                                              .build());
    }

    @AfterEach
    void tearDown() {
        lockRepository.deleteAll();
        courtRepository.deleteAll();
        userRepository.deleteAll();
        regionRepository.deleteAll();
        auditUserContext.clear();
    }

    @Test
    @DisplayName("Only one thread should successfully acquire the lock under concurrent access")
    void onlyOneThreadShouldSuccessfullyAcquireLockUnderConcurrentAccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Boolean> results = new CopyOnWriteArrayList<>();
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            User user = userRepository.save(User.builder()
                                                .email("lock-test-" + UUID.randomUUID() + "@justice.gov.uk")
                                                .ssoId(UUID.randomUUID())
                                                .role(UserRole.ADMIN)
                                                .build());

            threads[i] = new Thread(() -> {
                auditUserContext.setUserId(user.getId());
                synchronized (latch) {
                    try {
                        latch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                try {
                    lockService.createOrUpdateLock(SubjectType.COURT, testCourt.getId(), Page.GENERAL, user.getId());
                    results.add(true);
                } catch (ResponseStatusException ex) {
                    results.add(false);
                } finally {
                    auditUserContext.clear();
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        latch.countDown();

        for (Thread thread : threads) {
            thread.join(Duration.ofSeconds(10));
        }

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        long conflictCount = results.stream().filter(result -> !result).count();

        assertEquals(1, successCount);
        assertEquals(THREAD_COUNT - 1, conflictCount);
    }
}
