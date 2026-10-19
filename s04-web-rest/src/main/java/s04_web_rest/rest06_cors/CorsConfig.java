package s04_web_rest.rest06_cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig = GLOBALNA konfiguracja CORS dla adresów /api/shop/**. {@code WebMvcConfigurer} (konfigurator Spring MVC)
 * — interfejs z metodami „dopisz coś do konfiguracji MVC”; Boot zbiera wszystkie takie beany.
 * Sklep internetowy (frontend) działa pod http://localhost:3000, a API — pod http://localhost:8080: to RÓŻNE
 * pochodzenia (inny port), więc przeglądarka wymaga zgody serwera wyrażonej nagłówkami CORS.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/shop/**")                         // których adresów API dotyczy zgoda
                .allowedOrigins("http://localhost:3000")             // z jakich stron (pochodzeń) wolno wołać API
                .allowedMethods("GET", "POST", "PUT", "DELETE")      // jakimi metodami HTTP
                .allowedHeaders("Content-Type", "X-Shop-Id")         // jakie nagłówki może wysłać skrypt strony
                .exposedHeaders("Location", "X-Total-Count")         // jakie nagłówki odpowiedzi skrypt może PRZECZYTAĆ
                .maxAge(3600);                                       // ile sekund przeglądarka pamięta zgodę z preflight
    }
}
