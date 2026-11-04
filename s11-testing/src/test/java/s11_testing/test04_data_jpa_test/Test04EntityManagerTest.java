package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test04DataJpaTest: {@code TestEntityManager} — pomocnik testowy do pracy z kontekstem utrwalania
 * (persistence context = pamięć podręczna encji w bieżącej sesji; s08_data_jpa/Jpa01Entities). Najważniejsze
 * metody: {@code persistAndFlush}, {@code find}, {@code flush}, {@code clear}, {@code refresh}, {@code detach}.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test04")
class Test04EntityManagerTest {

    @Autowired
    TestEntityManager tem;

    @Autowired
    ProductRepository repository;

    @Autowired
    JdbcClient jdbc;

    private BigDecimal priceInDatabase(long id) {
        return jdbc.sql("SELECT price FROM product WHERE id = :id").param("id", id).query(BigDecimal.class).single();
    }

    @Test
    @DisplayName("Sekcja 2: persistAndFlush zapisuje encję W BAZIE od razu (INSERT), a find zwraca ją z kontekstu")
    void persistAndFlush() {
        Category home = tem.find(Category.class, 2L);

        Product saved = tem.persistAndFlush(new Product("Zeszyt", home, new BigDecimal("4.50"), 40));

        assertThat(saved.getId()).isNotNull();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product WHERE name = 'Zeszyt'").query(Long.class).single())
                .as("po flush wiersz jest już w bazie (w transakcji testu)").isEqualTo(1);
        assertThat(tem.find(Product.class, saved.getId())).isSameAs(saved);   // ta sama instancja z kontekstu
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — bez clear() findById zwraca obiekt Z PAMIĘCI, a nie z bazy; asercja nic nie dowodzi o bazie")
    void assertionFromMemoryTrap() {
        Product mouse = repository.findById(2L).orElseThrow();   // trafia do kontekstu utrwalania
        jdbc.sql("UPDATE product SET price = 1.00 WHERE id = 2").update();   // zmiana "obok" Hibernate, wprost w bazie

        assertThat(repository.findById(2L).orElseThrow().getPrice())
                .as("kontekst nie wie o UPDATE-cie — oddaje starą cenę").isEqualByComparingTo("59.00");
        assertThat(priceInDatabase(2)).isEqualByComparingTo("1.00");

        tem.clear();   // wyrzuć wszystko z kontekstu → następny odczyt idzie do bazy

        assertThat(repository.findById(2L).orElseThrow().getPrice()).isEqualByComparingTo("1.00");
        assertThat(mouse).as("stary obiekt jest już odłączony").isNotSameAs(repository.findById(2L).orElseThrow());
    }

    @Test
    @DisplayName("Sekcja 2: zmiana encji (dirty checking) trafia do bazy dopiero przy flush — flush() + clear() i dopiero wtedy asercja")
    void flushAndClearBeforeAssertion() {
        Product mouse = repository.findById(2L).orElseThrow();
        mouse.setPrice(new BigDecimal("49.00"));   // bez save — encja jest zarządzana

        assertThat(priceInDatabase(2)).as("jeszcze stara cena — brak flush").isEqualByComparingTo("59.00");

        tem.flush();   // wypchnij zmiany do bazy (UPDATE)
        tem.clear();   // zapomnij obiekty

        assertThat(priceInDatabase(2)).isEqualByComparingTo("49.00");
        assertThat(repository.findById(2L).orElseThrow().getPrice()).isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("Sekcja 2: refresh wczytuje encję ponownie z bazy, nadpisując zmiany w pamięci")
    void refresh() {
        Product mouse = repository.findById(2L).orElseThrow();
        jdbc.sql("UPDATE product SET price = 2.00 WHERE id = 2").update();

        tem.refresh(mouse);

        assertThat(mouse.getPrice()).isEqualByComparingTo("2.00");
    }
}
