package s04_web_rest.solutions.rest06_cors;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Rest06Cors. Bez {@code @Configuration} — test dokłada ją przez @Import. */
public class SolutionCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/exercise/**")
                .allowedOrigins("https://raporty.example.com")
                .allowedMethods("GET", "PUT")
                .allowedHeaders("Content-Type")
                .maxAge(600);
    }
}
