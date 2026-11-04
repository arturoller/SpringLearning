package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Rest05CrudApi — warstwa serwisu. To zwykły test jednostkowy (BEZ Springa): serwis nie zna HTTP,
 * więc można go sprawdzić najtańszym możliwym testem. To główna zaleta podziału controller → service.
 */
class Rest05ProductServiceTest {

    ProductService service = new ProductService();

    @Test
    @DisplayName("1. Lista jest posortowana po id; start: Kawa (1) i Herbata (2)")
    void listSortedById() {
        service.create(new ProductRequest("Ciastka", new BigDecimal("12.50")));
        service.delete(1);
        service.create(new ProductRequest("Sok", new BigDecimal("7.00")));

        assertThat(service.findAll()).extracting(Product::id).containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("2. create nadaje kolejne id (3, 4…), a find zwraca Optional — puste dla nieznanego id")
    void createAndFind() {
        Product first = service.create(new ProductRequest("Ciastka", new BigDecimal("12.50")));
        Product second = service.create(new ProductRequest("Sok", new BigDecimal("7.00")));

        assertThat(first.id()).isEqualTo(3);
        assertThat(second.id()).isEqualTo(4);
        assertThat(service.find(3)).contains(first);
        assertThat(service.find(99)).isEmpty();
    }

    @Test
    @DisplayName("3. replace podmienia nazwę i cenę (kod wewnętrzny zostaje); changePrice zmienia tylko cenę")
    void replaceAndChangePrice() {
        Product replaced = service.replace(1, new ProductRequest("Kawa ziarnista", new BigDecimal("39.00"))).orElseThrow();
        assertThat(replaced.name()).isEqualTo("Kawa ziarnista");
        assertThat(replaced.internalCode()).isEqualTo("SKU-1");

        Product cheaper = service.changePrice(1, new BigDecimal("29.00")).orElseThrow();
        assertThat(cheaper.name()).isEqualTo("Kawa ziarnista");
        assertThat(cheaper.price()).isEqualByComparingTo("29.00");

        assertThat(service.replace(99, new ProductRequest("x", BigDecimal.ONE))).isEmpty();
        assertThat(service.changePrice(99, BigDecimal.ONE)).isEmpty();
    }

    @Test
    @DisplayName("4. delete zwraca true tylko za pierwszym razem")
    void deleteTwice() {
        assertThat(service.delete(1)).isTrue();
        assertThat(service.delete(1)).isFalse();
        assertThat(service.find(1)).isEmpty();
    }
}
