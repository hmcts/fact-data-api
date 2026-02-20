package uk.gov.hmcts.reform.fact.data.api.config;

import uk.gov.hmcts.reform.fact.data.api.security.AuthService;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SpringSecurityConfiguration {

    private final AuthService authService;

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults())
            // ensure that there is at least a bearer token
            .authorizeHttpRequests(auth -> auth
                // expose the root to allows Azure's "Always On" functionality to work
                .requestMatchers("/").permitAll()
                // expose the openapi testing UI internally
                .requestMatchers("/swagger-ui/*", "/v3/api-docs", "/v3/api-docs/*").permitAll()
                // health endpoints are required by
                .requestMatchers("/health/*", "/health").permitAll()
                // everything else needs to have a valid Azure JWT
                .anyRequest().authenticated())
            .build();
    }
}
