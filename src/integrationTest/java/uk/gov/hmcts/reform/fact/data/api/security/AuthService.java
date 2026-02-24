package uk.gov.hmcts.reform.fact.data.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Allows full {@link org.springframework.boot.test.context.SpringBootTest} integration tests to function.
 */
@Service("authService")
@Primary
@Slf4j
public class AuthService {
    public boolean canView() {
        return true;
    }

    public boolean isAdmin() {
        return true;
    }
}
