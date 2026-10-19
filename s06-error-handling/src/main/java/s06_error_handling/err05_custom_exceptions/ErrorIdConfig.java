package s06_error_handling.err05_custom_exceptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/** Konfiguracja: produkcyjny generator identyfikatorów błędów (losowy UUID, skrócony do 8 znaków). */
@Configuration(proxyBeanMethods = false)
public class ErrorIdConfig {

    @Bean
    public ErrorIdGenerator errorIdGenerator() {
        return () -> UUID.randomUUID().toString().substring(0, 8);
    }
}
