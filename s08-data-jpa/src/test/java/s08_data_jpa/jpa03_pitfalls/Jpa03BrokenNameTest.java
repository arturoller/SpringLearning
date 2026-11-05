package s08_data_jpa.jpa03_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.repository.query.QueryCreationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Jpa03QueryMethods: literówka w nazwie metody zapytania zatrzymuje START aplikacji — błąd widać
 * od razu, a nie dopiero przy pierwszym wywołaniu metody.
 */
class Jpa03BrokenNameTest {

    @Test
    @DisplayName("Sekcja 3: findByNmae → aplikacja nie startuje: BeanCreationException ← QueryCreationException ← PropertyReferenceException")
    void typoInMethodNameStopsStartup() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(BrokenNameApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=jpa03",
                        "--spring.datasource.url=jdbc:h2:mem:jpa03-broken-" + UUID.randomUUID())
                .close())
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("brokenNameProductRepository")
                .cause().isInstanceOf(QueryCreationException.class)
                .cause().isInstanceOf(PropertyReferenceException.class)
                .hasMessage("No property 'nmae' found for type 'Product'; Did you mean 'name'");
    }
}
