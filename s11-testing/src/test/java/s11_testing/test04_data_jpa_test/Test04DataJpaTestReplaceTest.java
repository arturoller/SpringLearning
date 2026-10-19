package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test04DataJpaTest: podmiana bazy w wycinku ({@code @AutoConfigureTestDatabase}). Patrzymy na
 * adres połączenia (URL), z którym naprawdę rozmawia test. Domyślnie (Boot 4: {@code replace = NON_TEST}) baza
 * z konfiguracji jest podmieniana na świeżą wbudowaną — chyba że pochodzi z „testowego” źródła.
 */
class Test04DataJpaTestReplaceTest {

    static String urlOf(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    @Nested
    @DataJpaTest
    @ActiveProfiles("test04")
    class DefaultReplace {

        @Autowired
        DataSource dataSource;

        @Test
        @DisplayName("5. Domyślnie: adres jdbc:h2:mem:test04 z profilu jest IGNOROWANY — baza H2 o losowej nazwie")
        void replacedWithFreshEmbeddedDatabase() throws SQLException {
            assertThat(urlOf(dataSource))
                    .startsWith("jdbc:h2:mem:")
                    .isNotEqualTo("jdbc:h2:mem:test04");
        }
    }

    @Nested
    @DataJpaTest
    @ActiveProfiles("test04")
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    class NoReplace {

        @Autowired
        DataSource dataSource;

        @Test
        @DisplayName("5. replace = NONE: test używa bazy z konfiguracji (jdbc:h2:mem:test04)")
        void usesConfiguredDatabase() throws SQLException {
            assertThat(urlOf(dataSource)).isEqualTo("jdbc:h2:mem:test04");
        }
    }

    @Nested
    @DataJpaTest
    @ActiveProfiles("test04")
    class DynamicPropertyIsTestSource {

        @DynamicPropertySource   // adres bazy wyliczony W TEŚCIE = „testowe źródło” → NON_TEST go nie podmienia
        static void datasource(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:test04dynamic");
        }

        @Autowired
        DataSource dataSource;

        @Test
        @DisplayName("5. NON_TEST: adres z @DynamicPropertySource to źródło testowe — baza NIE jest podmieniana")
        void dynamicPropertyNotReplaced() throws SQLException {
            assertThat(urlOf(dataSource)).isEqualTo("jdbc:h2:mem:test04dynamic");
        }
    }
}
