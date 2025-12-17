package uk.gov.hmcts.reform.fact.data.api.config;

import uk.gov.hmcts.reform.fact.data.api.security.AuthService;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;
import lombok.RequiredArgsConstructor;
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
public class SpringSecurityConfig {

    private final AuthService authService;

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults())
            //.csrf(AbstractHttpConfigurer::disable)
            // ensure that there is at least a bearer token
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("swagger-ui/*", "v3/api-docs", "v3/api-docs/*").permitAll()
                .anyRequest().authenticated())
            .build();
    }

}
