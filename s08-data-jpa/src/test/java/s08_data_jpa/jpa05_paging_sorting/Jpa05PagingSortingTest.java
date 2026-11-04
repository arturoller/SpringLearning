package s08_data_jpa.jpa05_paging_sorting;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa05PagingSorting na wycinku {@code @DataJpaTest}. Dane: dwanaście produktów z migracji
 * db/migration/jpa05/V2 (ceny 24.50 i 59.00 się powtarzają). Liczbę zapytań liczą statystyki Hibernate,
 * a ich treść zapisuje SqlCapture (StatementInspector).
 */
@DataJpaTest(showSql = false,
        properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "s08_data_jpa.jpa05_paging_sorting.SqlCapture")
@ActiveProfiles("jpa05")
class Jpa05PagingSortingTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        SqlCapture.clear();
    }

    private static List<String> names(Iterable<Product> products) {
        return java.util.stream.StreamSupport.stream(products.spliterator(), false).map(Product::getName).toList();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: PageRequest.of(1, 2) to DRUGA strona — numeracja od 0; ujemny numer = IllegalArgumentException")
    void section1PagesFromZero() {
        Sort byPrice = Sort.by("price").and(Sort.by("id"));
        assertThat(names(repository.findByCategory(Category.COMPUTERS, PageRequest.of(0, 2, byPrice))))
                .containsExactly("Podkładka pod mysz", "Mysz");
        assertThat(names(repository.findByCategory(Category.COMPUTERS, PageRequest.of(1, 2, byPrice))))
                .containsExactly("Klawiatura", "Monitor");
        assertThat(SqlCapture.all()).anySatisfy(sql -> assertThat(sql).endsWith("offset ? rows fetch first ? rows only"));

        assertThatThrownBy(() -> PageRequest.of(-1, 2))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Page index must not be less than zero");
        assertThatThrownBy(() -> PageRequest.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Page size must not be less than one");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: Page — treść + liczba wszystkich wyników kosztem DRUGIEGO zapytania (count)")
    void section2PageRunsCount() {
        Page<Product> page = repository.findByCategory(Category.COMPUTERS, PageRequest.of(0, 2, Sort.by("id")));

        assertThat(names(page)).containsExactly("Klawiatura", "Mysz");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.isFirst()).isTrue();
        assertThat(stats.getPrepareStatementCount()).as("SELECT strony + SELECT count").isEqualTo(2);
        assertThat(SqlCapture.last()).isEqualTo("select count(*) from product p1_0 where p1_0.category=?");
    }

    @Test
    @DisplayName("Sekcja 2: Page bez count, gdy wynik sam mówi, ile jest wszystkiego (pierwsza niepełna albo ostatnia niepełna strona)")
    void section2PageSkipsCountWhenObvious() {
        Page<Product> onlyPage = repository.findByCategory(Category.HOME, PageRequest.of(0, 10, Sort.by("id")));
        assertThat(onlyPage.getTotalElements()).isEqualTo(3);
        assertThat(stats.getPrepareStatementCount()).as("3 < 10 na pierwszej stronie — count zbędny").isEqualTo(1);

        stats.clear();
        Page<Product> lastPartial = repository.findByCategory(Category.COMPUTERS, PageRequest.of(1, 3, Sort.by("id")));
        assertThat(names(lastPartial)).containsExactly("Laptop", "Podkładka pod mysz");
        assertThat(lastPartial.getTotalElements()).as("offset 3 + 2 wiersze = 5").isEqualTo(5);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);

        stats.clear();
        Page<Product> beyond = repository.findByCategory(Category.COMPUTERS, PageRequest.of(5, 2, Sort.by("id")));
        assertThat(beyond.getContent()).as("strona za końcem: pusta treść, bez wyjątku").isEmpty();
        assertThat(beyond.getTotalElements()).isEqualTo(5);
        assertThat(stats.getPrepareStatementCount()).as("pusta strona — count potrzebny").isEqualTo(2);
    }

    @Test
    @DisplayName("Sekcja 2: Slice — bez count; pobiera size + 1 wierszy, żeby wiedzieć, czy jest następna strona")
    void section2SliceNoCount() {
        Slice<Product> slice = repository.findByStockGreaterThan(0, PageRequest.of(0, 3, Sort.by("id")));

        assertThat(names(slice)).containsExactly("Klawiatura", "Mysz", "Monitor");
        assertThat(slice.hasNext()).isTrue();
        assertThat(stats.getPrepareStatementCount()).as("jedno zapytanie, bez count").isEqualTo(1);
        assertThat(stats.getEntityLoadCount()).as("wczytane 4 = size + 1 (czwarty tylko sprawdza „czy jest dalej”)").isEqualTo(4);

        Slice<Product> last = repository.findByStockGreaterThan(0, PageRequest.of(3, 3, Sort.by("id")));
        assertThat(names(last)).containsExactly("Segregator");
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Sekcja 2: List z Pageable — sama treść strony, jedno zapytanie")
    void section2ListWithPageable() {
        List<Product> page = repository.findByPriceLessThan(new BigDecimal("100"),
                PageRequest.of(1, 3, Sort.by("price", "id")));

        assertThat(names(page)).containsExactly("Podkładka pod mysz", "Mysz", "Kubek termiczny");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: Sort — kierunek, kilka pól, Sort.Order; ostatnie pole id = kolejność jednoznaczna")
    void section3Sort() {
        assertThat(names(repository.findByCategory(Category.OFFICE, Sort.by(Sort.Direction.DESC, "price").and(Sort.by("id")))))
                .containsExactly("Krzesło biurowe", "Segregator", "Zeszyt", "Długopis");
        assertThat(SqlCapture.last()).endsWith("order by p1_0.price desc,p1_0.id");

        assertThat(names(repository.findAll(Sort.by("price").descending().and(Sort.by("id")))))
                .as("remisy 59.00 i 24.50 rozstrzyga id")
                .containsSubsequence("Mysz", "Kubek termiczny", "Segregator")
                .containsSubsequence("Kubek", "Podkładka pod mysz");

        Sort mixed = Sort.by(Sort.Order.asc("price"), Sort.Order.desc("name"));
        assertThat(names(repository.findByCategory(Category.COMPUTERS, mixed))).first().isEqualTo("Podkładka pod mysz");
        assertThat(SqlCapture.last()).endsWith("order by p1_0.price,p1_0.name desc");
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKA — nieznane pole w Sort to błąd w CZASIE WYKONANIA (PropertyReferenceException), nie przy starcie")
    void section3UnknownSortProperty() {
        assertThatThrownBy(() -> repository.findByCategory(Category.OFFICE, PageRequest.of(0, 2, Sort.by("prize"))))
                .isInstanceOf(PropertyReferenceException.class)
                .hasMessage("No property 'prize' found for type 'Product'; Did you mean 'price'");
        assertThat(stats.getPrepareStatementCount()).as("do bazy nic nie poszło").isZero();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: @Query + Pageable — count wyprowadzony automatycznie albo z countQuery")
    void section4QueryWithPageable() {
        PageRequest firstThree = PageRequest.of(0, 3, Sort.by("price").and(Sort.by("id")));

        Page<Product> auto = repository.findPricedFrom(new BigDecimal("59"), firstThree);
        assertThat(names(auto)).containsExactly("Mysz", "Kubek termiczny", "Segregator");
        assertThat(auto.getTotalElements()).isEqualTo(8);
        assertThat(SqlCapture.last()).isEqualTo("select count(*) from product p1_0 where p1_0.price>=?");

        Page<Product> own = repository.findPricedFromWithCountQuery(new BigDecimal("59"), firstThree);
        assertThat(own.getTotalElements()).isEqualTo(8);
        assertThat(SqlCapture.last()).as("nasze countQuery").isEqualTo("select count(p1_0.id) from product p1_0 where p1_0.price>=?");
    }

    @Test
    @DisplayName("Sekcja 4: Limit jako argument i Top10 w nazwie — fetch first n rows only")
    void section4LimitAndTop() {
        assertThat(names(repository.findByCategoryOrderByPriceAscIdAsc(Category.COMPUTERS, Limit.of(2))))
                .containsExactly("Podkładka pod mysz", "Mysz");
        assertThat(SqlCapture.last()).endsWith("fetch first ? rows only");

        assertThat(repository.findByCategoryOrderByPriceAscIdAsc(Category.COMPUTERS, Limit.unlimited()))
                .as("Limit.unlimited() — bez ograniczenia").hasSize(5);
        assertThat(SqlCapture.last()).doesNotContain("fetch first");

        assertThat(repository.findTop10ByOrderByPriceDescIdAsc()).hasSize(10).first()
                .extracting(Product::getName).isEqualTo("Laptop");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: keyset — Window i ScrollPosition: kolejne okno zaczyna się „po” ostatnim kluczu (where price > ? or …)")
    void section5KeysetScrolling() {
        Window<Product> first = repository.findFirst4ByOrderByPriceAscIdAsc(ScrollPosition.keyset());
        assertThat(names(first)).containsExactly("Długopis", "Zeszyt", "Kubek", "Podkładka pod mysz");
        assertThat(first.hasNext()).isTrue();

        ScrollPosition afterLast = first.positionAt(first.size() - 1);
        assertThat(afterLast).isInstanceOfSatisfying(KeysetScrollPosition.class,
                keyset -> assertThat(keyset.getKeys()).isEqualTo(Map.of("price", new BigDecimal("24.50"), "id", 11L)));

        Window<Product> second = repository.findFirst4ByOrderByPriceAscIdAsc(afterLast);
        assertThat(names(second)).containsExactly("Mysz", "Kubek termiczny", "Segregator", "Lampka");
        assertThat(SqlCapture.last()).as("bez OFFSET — warunek na kluczu")
                .contains("where p1_0.price>? or p1_0.price=? and p1_0.id>?")
                .doesNotContain("offset");

        Window<Product> third = repository.findFirst4ByOrderByPriceAscIdAsc(second.positionAt(second.size() - 1));
        assertThat(names(third)).containsExactly("Klawiatura", "Krzesło biurowe", "Monitor", "Laptop");
        assertThat(third.hasNext()).isFalse();
    }
}
