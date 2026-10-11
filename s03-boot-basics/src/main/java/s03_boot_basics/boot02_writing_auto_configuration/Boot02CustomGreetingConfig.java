package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.context.annotation.Bean;

/**
 * Boot02CustomGreetingConfig = konfiguracja „użytkownika biblioteki” z WŁASNYM GreetingService.
 * Bez {@code @Configuration} — test rejestruje ją ręcznie (ApplicationContextRunner.withUserConfiguration), żeby nie
 * zmieniała kontekstu Boot02Application.
 */
public class Boot02CustomGreetingConfig {

    @Bean
    GreetingService vipGreetingService() {
        return customer -> "Witamy ponownie, " + customer + "! Klient VIP ma kawę gratis.";
    }
}
