package s00_start.start02_multi_module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Start02MultiModule.
 * {@code @SpringBootTest} (test całej aplikacji Spring Boot) szuka klasy z {@code @SpringBootApplication} w tym pakiecie
 * i wyżej — znajduje Start02Application, uruchamia jej kontekst i wstrzykuje go do pola z {@code @Autowired}
 * (autowired = automatycznie powiązany/wstrzyknięty).
 */
@SpringBootTest
class Start02MultiModuleTest {

    @Autowired
    ApplicationContext context;   // context = kontekst aplikacji, czyli kontener ze wszystkimi beanami

    @Test
    @DisplayName("2. W module s00-start jest Spring MVC, ale nie ma Spring Security ani Spring Data JPA")
    void moduleClasspath() {
        assertThat(Start02MultiModule.isOnClasspath("org.springframework.web.servlet.DispatcherServlet")).isTrue();
        assertThat(Start02MultiModule.isOnClasspath(
                "org.springframework.security.config.annotation.web.builders.HttpSecurity")).isFalse();
        assertThat(Start02MultiModule.isOnClasspath("org.springframework.data.jpa.repository.JpaRepository"))
                .isFalse();   // pytanie kontrolne 2
    }

    @Test
    @DisplayName("3. Autokonfiguracja dodała dispatcherServlet, choć aplikacja nie ma żadnego własnego beana")
    void autoConfigurationAddsWebBeans() {
        assertThat(context.containsBean("dispatcherServlet")).isTrue();
    }

    @Test
    @DisplayName("3. Bez bibliotek bazy danych na classpath Boot NIE tworzy beana DataSource")
    void noDatabaseNoDataSource() {
        // getBeanNamesForType = nazwy beanów danego typu; pusta tablica = takiego beana nie ma
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
    }

    @Test
    @DisplayName("3. Mapa starterów używa nazw z Boot 4 (webmvc zamiast web)")
    void boot4StarterNames() {
        assertThat(Start02MultiModule.STARTERS).containsEntry("web", "spring-boot-starter-webmvc");
    }
}
