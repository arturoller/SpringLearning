package s08_data_jpa.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Jpa02Repositories. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy albo
 * {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}).
 */
@DataJpaTest
@ActiveProfiles("jpa02")
@ExtendWith(OutputCaptureExtension.class)
@Tag("cwiczenie")
class Jpa02RepositoriesExercisesTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    GiftCardRepository giftCardRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Ćwiczenie 1: dostawa zwiększa stan magazynowy; nieznane id → IllegalArgumentException")
    void exercise1() {
        Long id = entityManager.persistAndGetId(
                new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7), Long.class);
        entityManager.flush();
        entityManager.clear();

        assertThat(Jpa02Repositories.exercise1(productRepository, id, 5)).isEqualTo(12);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Product.class, id).getStock()).isEqualTo(12);

        assertThatThrownBy(() -> Jpa02Repositories.exercise1(productRepository, -1L, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Brak produktu o id -1");
    }

    @Test
    @DisplayName("Ćwiczenie 2: usuwa produkty z zerowym stanem i zwraca ich liczbę")
    void exercise2() {
        productRepository.saveAll(List.of(
                new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7),
                new Product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, new BigDecimal("2999.00"), 0),
                new Product("SPO-003", "Oliwa z oliwek", Category.SPOZYWCZE, new BigDecimal("42.00"), 0),
                new Product("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"), 15)));
        entityManager.flush();
        entityManager.clear();

        assertThat(Jpa02Repositories.exercise2(productRepository)).isEqualTo(2);
        entityManager.flush();
        entityManager.clear();
        assertThat(productRepository.findAll()).extracting(Product::getSku)
                .containsExactlyInAnyOrder("ELE-001", "KSI-001");
    }

    @Test
    @DisplayName("Ćwiczenie 3: Persistable — save nowej karty bez SELECT, save odłączonej karty robi UPDATE")
    void exercise3(CapturedOutput output) {
        int start = output.length();
        giftCardRepository.save(new GiftCard("KARTA-100", new BigDecimal("100.00")));
        entityManager.flush();
        String sql = output.toString().substring(start);
        assertThat(sql).contains("Hibernate: insert into gift_cards");
        assertThat(sql).as("nowa karta nie powinna wymagać SELECT").doesNotContain("Hibernate: select");

        entityManager.clear();
        GiftCard loaded = giftCardRepository.findById("KARTA-100").orElseThrow();
        entityManager.clear();                                     // karta odłączona — save musi zrobić merge
        loaded.setBalance(new BigDecimal("40.00"));
        giftCardRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(GiftCard.class, "KARTA-100").getBalance()).isEqualByComparingTo("40.00");
    }
}
