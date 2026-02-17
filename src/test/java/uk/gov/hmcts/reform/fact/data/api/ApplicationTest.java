package uk.gov.hmcts.reform.fact.data.api;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ApplicationTest {
    @Test
    void contextLoads() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            String[] args = new String[]{""};
            Application.main(args);
            springApp.verify(() -> SpringApplication.run(Application.class, args), times(1));
        }
    }
}
