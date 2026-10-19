package s14_openapi.api03_groups_and_versions;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracja dokumentacji: informacje ogólne (bean {@code OpenAPI}), schemat zabezpieczeń (tylko opis — w tym module nie
 * ma Spring Security) oraz grupy dokumentów ({@code GroupedOpenApi}).
 */
@Configuration
public class OpenApiConfig {

    /** Nazwa schematu zabezpieczeń w dokumencie — odwołują się do niej wymagania zabezpieczeń operacji. */
    static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI shopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sklep — API")
                        .version("2.0.0")
                        .description("API sklepu: produkty (publiczne) i administracja (wymaga tokenu).")
                        .contact(new Contact().name("Zespół sklepu").email("api@sklep.example"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT w nagłówku Authorization: Bearer ...")));
    }

    @Bean
    GroupedOpenApi publicGroup() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Publiczne")
                .pathsToMatch("/api/**")
                .build();
    }

    /** Obejście: osobny dokument dla każdej wersji API — z filtrem wybierającym metody obsługujące daną wersję. */
    @Bean
    GroupedOpenApi version1Group() {
        return GroupedOpenApi.builder()
                .group("v1")
                .displayName("Wersja 1")
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(new VersionGroupFilter(1))
                .build();
    }

    @Bean
    GroupedOpenApi version2Group() {
        return GroupedOpenApi.builder()
                .group("v2")
                .displayName("Wersja 2")
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(new VersionGroupFilter(2))
                .build();
    }

    @Bean
    GroupedOpenApi adminGroup() {
        OpenApiCustomizer requireToken = openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(op -> op.addSecurityItem(new SecurityRequirement().addList(BEARER))));
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Administracja")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(requireToken)
                .build();
    }
}
