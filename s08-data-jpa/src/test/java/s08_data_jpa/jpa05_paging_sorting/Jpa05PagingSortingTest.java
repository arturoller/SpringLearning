package s08_data_jpa.jpa05_paging_sorting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa05PagingSorting. {@code @DataJpaTest} + 14 produktów sklepu przed każdym testem. Dodatkowo
 * włączamy logger wartości parametrów (org.hibernate.orm.jdbc.bind), żeby zobaczyć, jaki LIMIT dostaje baza.
 */
@DataJpaTest(properties = "logging.level.org.hibernate.orm.jdbc.bind=trace")
@ActiveProfiles("jpa05")
@ExtendWith(OutputCaptureExtension.class)
class Jpa05PagingSortingTest {

    private static final String SELECT = "Hibernate: select p1_0.id,p1_0.category,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock "
            + "from products p1_0 ";

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    private static String since(CapturedOutput output, int start) {
        return output.toString().substring(start);
    }

    private static List<String> skus(Iterable<Product> products) {
        return java.util.stream.StreamSupport.stream(products.spliterator(), false).map(Product::getSku).toList();
    }

    @Test
    @DisplayName("1. Sort z zewnątrz: kilka kluczy, kierunek, ignoreCase; złe pole wychodzi dopiero przy wywołaniu")
    void sorting(CapturedOutput output) {
        int start = output.length();
        assertThat(skus(repository.findByPriceLessThan(new BigDecimal("100"), Sort.by(Sort.Direction.DESC, "price"))))
                .containsExactly("KSI-003", "KSI-001", "SPO-001", "ODZ-002", "SPO-003", "SPO-002");
        assertThat(skus(repository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase())))).first().isEqualTo("SPO-002");
        assertThat(since(output, start))
                .contains(SELECT + "where p1_0.price<? order by p1_0.price desc")
                .contains(SELECT + "order by lower(p1_0.name)");

        assertThatThrownBy(() -> repository.findAll(PageRequest.of(0, 5, Sort.by("cena"))))
                .isInstanceOf(PropertyReferenceException.class)
                .hasMessage("No property 'cena' found for type 'Product'");
    }

    @Test
    @DisplayName("2. Page: OFFSET/FETCH + osobne count; strony od zera; strona za końcem pusta; count pomijany, gdy zbędny")
    void pages(CapturedOutput output) {
        Sort byPriceDesc = Sort.by("price").descending().and(Sort.by("sku"));

        int start = output.length();
        Page<Product> first = repository.findAll(PageRequest.of(0, 5, byPriceDesc));
        assertThat(skus(first)).containsExactly("ELE-001", "ELE-002", "DOM-001", "ELE-004", "ODZ-001");
        assertThat(first.getTotalElements()).isEqualTo(14);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.getNumber()).isZero();
        assertThat(first.hasNext()).isTrue();
        assertThat(first.toString()).startsWith("Page 1 of 3");                 // dla człowieka — od jedynki
        assertThat(first.nextPageable()).isEqualTo(PageRequest.of(1, 5, byPriceDesc));
        assertThat(first.map(Product::getName).getContent()).first().isEqualTo("Laptop Pro 14");
        assertThat(since(output, start))
                .contains(SELECT + "order by p1_0.price desc,p1_0.sku offset ? rows fetch first ? rows only")
                .contains("Hibernate: select count(p1_0.id) from products p1_0");

        Page<Product> last = repository.findAll(PageRequest.of(2, 5, byPriceDesc));
        assertThat(skus(last)).containsExactly("SPO-001", "ODZ-002", "SPO-003", "SPO-002");
        assertThat(last.isLast()).isTrue();

        Page<Product> beyond = repository.findAll(PageRequest.of(5, 5));
        assertThat(beyond.getContent()).isEmpty();
        assertThat(beyond.getTotalElements()).isEqualTo(14);

        // pierwsza strona niepełna → Spring Data zna wynik bez count
        start = output.length();
        Page<Product> books = repository.findByCategory(Category.KSIAZKI, PageRequest.of(0, 5, Sort.by("price")));
        assertThat(books.getTotalElements()).isEqualTo(3);
        assertThat(since(output, start)).doesNotContain("count(");

        assertThatThrownBy(() -> PageRequest.of(-1, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Page index must not be less than zero");
        assertThatThrownBy(() -> PageRequest.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Page size must not be less than one");
    }

    @Test
    @DisplayName("3. Slice pobiera rozmiar+1 wierszy i nie liczy wszystkich; Limit i unpaged")
    void slices(CapturedOutput output) {
        int start = output.length();
        Slice<Product> slice = repository.findByStockGreaterThan(0, PageRequest.of(0, 5, Sort.by("sku")));
        String sql = since(output, start);
        assertThat(skus(slice)).containsExactly("DOM-001", "DOM-002", "ELE-001", "ELE-003", "ELE-004");
        assertThat(slice.hasNext()).isTrue();
        assertThat(sql).contains(SELECT + "where p1_0.stock>? order by p1_0.sku fetch first ? rows only");
        assertThat(sql).as("limit = rozmiar + 1").contains("binding parameter (2:INTEGER) <- [6]");
        assertThat(sql).doesNotContain("count(");

        assertThat(skus(repository.findByCategoryOrderByPriceDesc(Category.ELEKTRONIKA, Limit.of(2))))
                .containsExactly("ELE-001", "ELE-002");
        Page<Product> all = repository.findAll(Pageable.unpaged());
        assertThat(all.getContent()).hasSize(14);
    }

    @Test
    @DisplayName("4. Remis na granicy stron: samo sortowanie po cenie nie wyznacza kolejności; unikalny drugi klucz (sku) ją ustala")
    void tiesNeedTieBreaker() {
        // ŹLE: tylko po cenie. 7. i 8. pozycja kosztują po 129.00 — granica stron leży w środku remisu.
        Page<Product> page0 = repository.findAll(PageRequest.of(0, 7, Sort.by("price")));
        Page<Product> page1 = repository.findAll(PageRequest.of(1, 7, Sort.by("price")));
        assertThat(page0.getContent().getLast().getPrice()).isEqualByComparingTo("129.00");
        assertThat(page1.getContent().getFirst().getPrice()).isEqualByComparingTo("129.00");
        // KTÓRE produkty stoją na granicy — tego test celowo NIE sprawdza: H2 przy OFFSET/FETCH sortuje częściowo,
        // z losowym elementem osiowym, i w kolejnych uruchomieniach raz pokazywał KSI-002 na obu stronach (bez DOM-002),
        // raz odwrotnie, a raz poprawnie. Asercja na tym byłaby losowa — i właśnie o tym jest ta pułapka.

        // DOBRZE: unikalny klucz na końcu — dziesięć razy z rzędu ten sam, kompletny podział na strony
        Sort stable = Sort.by("price").and(Sort.by("sku"));
        for (int attempt = 0; attempt < 10; attempt++) {
            List<String> bothPages = new ArrayList<>(skus(repository.findAll(PageRequest.of(0, 7, stable))));
            bothPages.addAll(skus(repository.findAll(PageRequest.of(1, 7, stable))));
            assertThat(bothPages).containsExactly("SPO-002", "SPO-003", "ODZ-002", "SPO-001", "KSI-001", "KSI-003",
                    "DOM-002", "KSI-002", "ELE-003", "ODZ-001", "ELE-004", "DOM-001", "ELE-002", "ELE-001");
        }
    }

    @Test
    @DisplayName("5. @Query + Pageable: ORDER BY z Pageable i zapytanie count wyprowadzone z JPQL")
    void queryWithPageable(CapturedOutput output) {
        int start = output.length();
        Page<Product> page = repository.findPricedFrom(new BigDecimal("100"), PageRequest.of(0, 3, Sort.by("price").and(Sort.by("sku"))));
        assertThat(skus(page)).containsExactly("DOM-002", "KSI-002", "ELE-003");
        assertThat(page.getTotalElements()).isEqualTo(8);
        assertThat(since(output, start))
                .contains(SELECT + "where p1_0.price>=? order by p1_0.price,p1_0.sku fetch first ? rows only")
                .contains("Hibernate: select count(*) from products p1_0 where p1_0.price>=?");
    }
}
