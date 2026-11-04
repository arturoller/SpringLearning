package s09_jpa_relations.rel03_many_to_many;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Rel03ManyToMany (ProductTagExercises). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Uwaga: {@code @Tag} JUnit (etykieta testu) to co innego niż encja {@code Tag} lekcji — dlatego encję piszemy tu
 * z pełną nazwą pakietu.
 */
@Tag("cwiczenie")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel03")
@Import(ProductTagExercises.class)
class Rel03ManyToManyExercisesTest {

    @Autowired
    private ProductTagExercises exercises;

    @Autowired
    private TestEntityManager tem;

    @BeforeEach
    void setUp() {
        SqlCapture.clear();
    }

    private List<String> tagNamesOf(Long productId) {
        tem.flush();
        tem.clear();
        return tem.find(Product.class, productId).getTags().stream()
                .map(s09_jpa_relations.rel03_many_to_many.Tag::getName).sorted().toList();
    }

    @Test
    @DisplayName("Ćwiczenie 1: tag istniejący i nowy (tworzony), powtórne dodanie bez zmian")
    void exercise1() {
        exercises.tagProduct(4L, "nowość");      // istniejący tag
        exercises.tagProduct(4L, "kuchnia");     // nowy tag
        exercises.tagProduct(4L, "promocja");    // Kubek już go ma

        assertThat(tagNamesOf(4L)).containsExactly("kuchnia", "nowość", "promocja");
        assertThat(SqlCapture.startingWith("insert into tag")).as("jeden nowy tag").hasSize(1);
    }

    @Test
    @DisplayName("Ćwiczenie 2: zdjęcie tagu — dokładnie jeden DELETE z product_tag")
    void exercise2() {
        exercises.untagProduct(2L, "gaming");
        exercises.untagProduct(2L, "nie-ma-takiego");
        SqlCapture.clear();
        tem.flush();

        assertThat(SqlCapture.all()).containsExactly("delete from product_tag where product_id=? and tag_id=?");
        assertThat(tagNamesOf(2L)).containsExactly("bezprzewodowe", "biuro", "promocja");
    }

    @Test
    @DisplayName("Ćwiczenie 3: nazwy produktów z którymkolwiek tagiem — bez powtórzeń, alfabetycznie")
    void exercise3() {
        assertThat(exercises.productNamesWithAnyTag(List.of("biuro", "promocja")))
                .containsExactly("Klawiatura", "Kubek", "Lampka", "Mysz");
        assertThat(exercises.productNamesWithAnyTag(List.of("nowość"))).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 4: nazwy produktów z WSZYSTKIMI tagami")
    void exercise4() {
        assertThat(exercises.productNamesWithAllTags(List.of("biuro", "gaming"))).containsExactly("Klawiatura", "Mysz");
        assertThat(exercises.productNamesWithAllTags(List.of("biuro", "gaming", "promocja"))).containsExactly("Mysz");
        assertThat(exercises.productNamesWithAllTags(List.of("gaming"))).containsExactly("Klawiatura", "Monitor", "Mysz");
    }

    @Test
    @DisplayName("Ćwiczenie 5: najtańszy dostawca produktu albo pusty Optional")
    void exercise5() {
        assertThat(exercises.cheapestSupplierName(1L)).contains("Hurtownia Beta");
        assertThat(exercises.cheapestSupplierName(2L)).contains("Hurtownia Alfa");
        assertThat(exercises.cheapestSupplierName(3L)).isEmpty();
    }
}
