package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@Accessors(chain = true)
public class RedisServerConfigurationProperties {

    /**
     * Redis server host.
     */
    @NotBlank
    String host;

    /**
     * Redis server port.
     */
    int port = 6379;

    /**
     * Redis server password.
     */
    String password;
}
