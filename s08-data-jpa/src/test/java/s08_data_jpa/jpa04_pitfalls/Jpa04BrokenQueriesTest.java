package s08_data_jpa.jpa04_pitfalls;

import org.hibernate.query.sqm.UnknownEntityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.repository.query.QueryCreationException;
import s08_data_jpa.jpa04_pitfalls.jpql.BrokenJpqlApplication;
import s08_data_jpa.jpa04_pitfalls.nativesql.NativeTypoApplication;
import s08_data_jpa.jpa04_pitfalls.nativesql.NativeTypoRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1 i 2 lekcji Jpa04CustomQueries: KIEDY wychodzi błąd w zapytaniu z {@code @Query}. JPQL Spring Data sprawdza
 * przy STARCIE (Hibernate parsuje zapytanie), natywnego SQL — nie: błąd wychodzi dopiero przy wywołaniu metody.
 */
class Jpa04BrokenQueriesTest {

    private static ConfigurableApplicationContext start(Class<?> application) {
        return new SpringApplicationBuilder(application)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=jpa04",
                        "--spring.datasource.url=jdbc:h2:mem:jpa04-broken-" + UUID.randomUUID());
    }

    @Test
    @DisplayName("Sekcja 1: nazwa TABELI w JPQL → aplikacja nie startuje (Query validation failed ← UnknownEntityException)")
    void tableNameInJpqlStopsStartup() {
        assertThatThrownBy(() -> start(BrokenJpqlApplication.class).close())
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("tableNameInJpqlRepository")
                .cause().isInstanceOf(QueryCreationException.class)
                .hasMessageContaining("Query validation failed for 'select p from product p where p.stock = 0'")
                .hasRootCauseInstanceOf(UnknownEntityException.class)
                .rootCause().hasMessage("Could not resolve root entity 'product'");
    }

    @Test
    @DisplayName("Sekcja 2: literówka w natywnym SQL → aplikacja STARTUJE, błąd dopiero przy wywołaniu metody")
    void typoInNativeSqlFailsOnlyAtCall() {
        try (ConfigurableApplicationContext app = start(NativeTypoApplication.class)) {
            NativeTypoRepository repository = app.getBean(NativeTypoRepository.class);
            assertThat(repository).as("kontekst wystartował mimo błędu w SQL").isNotNull();

            assertThatThrownBy(repository::findOutOfStock)
                    .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                    .hasMessageContaining("Column \"STOK\" not found");
        }
    }
}
