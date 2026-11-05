package s03_boot_basics.boot04_starters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringVersion;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Boot04Starters — jeden test na sekcję.
 * <p>
 * Najwęższe narzędzie, które wystarcza: sekcje 1–3 to zwykłe testy jednostkowe (sprawdzamy classpath i wersje, bez
 * kontekstu Springa), sekcja 2 dodatkowo używa {@link ApplicationContextRunner} (Boot01AutoConfiguration) do
 * „odinstalowania” biblioteki, a sekcja 4 uruchamia prawdziwą aplikację {@link Boot04Application}.
 */
class Boot04StartersTest {

    /** Konfiguracja UŻYTKOWNIKA zależna od obecności klasy na classpath (sekcja 2). */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.h2.Driver")
    static class H2AwareConfiguration {
        @Bean
        String databaseKind() {
            return "H2";
        }
    }

    @Test
    @DisplayName("1. Starter JDBC przyniósł klasy puli, JdbcTemplate i autokonfiguracji; starter webowy — nie")
    void section1_starterBringsClasses() {
        ClassLoader loader = getClass().getClassLoader();
        // to, co dał spring-boot-starter-jdbc (przechodnio): moduł spring-boot-jdbc, spring-jdbc i HikariCP
        assertThat(ClassUtils.isPresent("org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", loader)).isTrue();
        assertThat(ClassUtils.isPresent("org.springframework.jdbc.core.JdbcTemplate", loader)).isTrue();
        assertThat(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", loader)).isTrue();
        // starter webmvc niczego nie dodał — nie ma wpisu w pom.xml tego modułu
        assertThat(ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", loader)).isFalse();

        // aplikacja dostaje DataSource „z samego startera” — bez linijki konfiguracji
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Boot04Application.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(context.getBeanNamesForType(DataSource.class)).hasSize(1);
        }
    }

    @Test
    @DisplayName("2. Boot zgaduje z classpath: są klasy bazy, nie ma Tomcata, więc typ aplikacji to NONE")
    void section2_classpathDetection() {
        ClassLoader loader = getClass().getClassLoader();
        assertThat(ClassUtils.isPresent("org.h2.Driver", loader)).isTrue();
        assertThat(ClassUtils.isPresent("org.apache.catalina.startup.Tomcat", loader)).isFalse();
        assertThat(ClassUtils.isPresent("jakarta.servlet.Servlet", loader)).isFalse();

        // wynik wykrywania: brak klas webowych → zwykły program
        assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.NONE);
        assertThat(new SpringApplication(Boot04Application.class).getWebApplicationType()).isEqualTo(WebApplicationType.NONE);

        // FilteredClassLoader „odinstalowuje” bibliotekę: ten sam kod zachowuje się inaczej, gdy klasy H2 brakuje
        ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(H2AwareConfiguration.class);
        runner.run(context -> assertThat(context).hasSingleBean(String.class).hasBean("databaseKind"));
        runner.withClassLoader(new FilteredClassLoader("org.h2"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("databaseKind");
                });
    }

    @Test
    @DisplayName("3. Wersje pochodzą z BOM: Spring Boot 4.x i Spring Framework 7.x")
    void section3_versionsFromBom() {
        assertThat(SpringBootVersion.getVersion()).startsWith("4.");
        assertThat(SpringVersion.getVersion()).startsWith("7.");
    }

    @Test
    @DisplayName("4. DevTools jest na classpath, ale w teście jest wyłączony i niczego nie zmienia w aplikacji")
    void section4_devToolsDisabledInTests() {
        // zależność jest (optional nie znaczy „brak” — działa lokalnie, tylko nie przechodzi dalej)
        assertThat(ClassUtils.isPresent("org.springframework.boot.devtools.restart.Restarter",
                getClass().getClassLoader())).isTrue();

        // DevTools rozpoznaje test po śladzie stosu wątku (klasy JUnit) i sam się wyłącza
        assertThat(DevToolsEnablementDeducer.shouldEnable(Thread.currentThread())).isFalse();

        // skutek: uruchomiona aplikacja nie dostała ustawień deweloperskich
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Boot04Application.class)
                .web(WebApplicationType.NONE).run()) {
            assertThat(context.getEnvironment().getPropertySources().contains("devtools")).isFalse();
        }
    }

    /** Konwencja: przedrostek spring-boot-starter jest zarezerwowany dla projektu Spring Boot. */
    private static boolean reservedForSpring(String artifactId) {
        return artifactId.startsWith("spring-boot-starter");
    }

    @Test
    @DisplayName("5. Własny starter: nazwy spring-boot-starter-* są zarezerwowane dla Springa, własne mają nazwę na początku")
    void section5_starterNaming() {
        assertThat(reservedForSpring("spring-boot-starter-jdbc")).isTrue();
        assertThat(reservedForSpring("spring-boot-starter-acme")).isTrue(); // tak NIE nazywaj własnego startera
        assertThat(reservedForSpring("acme-spring-boot-starter")).isFalse();
        assertThat(reservedForSpring("acme-spring-boot-autoconfigure")).isFalse();
    }
}
