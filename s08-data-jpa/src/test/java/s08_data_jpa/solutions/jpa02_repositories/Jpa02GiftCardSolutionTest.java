package s08_data_jpa.solutions.jpa02_repositories;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenie ćwiczenia 3 z Jpa02RepositoriesExercisesTest na rozwiązaniu wzorcowym (GiftCard implements Persistable).
 * {@code @Tag("wzorzec")}. Kontekst: Jpa02SolutionsApplication z tego pakietu (karta z rozwiązania, nie szkielet).
 */
@DataJpaTest
@ActiveProfiles("jpa02")
@ExtendWith(OutputCaptureExtension.class)
@Tag("wzorzec")
class Jpa02GiftCardSolutionTest {

    @Autowired
    GiftCardRepository giftCardRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Wzorzec 3: Persistable — save nowej karty bez SELECT, save odłączonej karty robi UPDATE")
    void solution3(CapturedOutput output) {
        int start = output.length();
        giftCardRepository.save(new GiftCard("KARTA-100", new BigDecimal("100.00")));
        entityManager.flush();
        String sql = output.toString().substring(start);
        assertThat(sql).contains("Hibernate: insert into gift_cards");
        assertThat(sql).doesNotContain("Hibernate: select");

        entityManager.clear();
        GiftCard loaded = giftCardRepository.findById("KARTA-100").orElseThrow();
        assertThat(loaded.isNew()).as("@PostLoad ustawił flagę").isFalse();
        entityManager.clear();
        loaded.setBalance(new BigDecimal("40.00"));
        giftCardRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(GiftCard.class, "KARTA-100").getBalance()).isEqualByComparingTo("40.00");
    }
}
