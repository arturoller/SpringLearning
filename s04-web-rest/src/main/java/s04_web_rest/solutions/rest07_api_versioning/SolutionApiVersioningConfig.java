package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Wersjonowanie dla aplikacji rozwiązań — jak ApiVersioningConfig w lekcji (nagłówek API-Version). */
@Configuration
public class SolutionApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader("API-Version").addSupportedVersions("1.0", "2.0");
    }
}
