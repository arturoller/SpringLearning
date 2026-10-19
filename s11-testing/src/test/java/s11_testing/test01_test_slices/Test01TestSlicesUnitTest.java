package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test01TestSlices: podstawa piramidy testów — test jednostkowy (unit test) BEZ Springa.
 * Żadnej adnotacji Springa, żadnego kontekstu: zwykłe {@code new} i asercja. Taki test trwa milisekundy,
 * więc może ich być setki.
 */
class Test01TestSlicesUnitTest {

    @Test
    @DisplayName("1. Test jednostkowy: encja → DTO, 6499 gr → \"64.99\" — bez Springa, w milisekundach")
    void dtoFromEntityWithoutSpring() {
        Product coffee = new Product("Kawa ziarnista 1kg", 6499);

        ProductDto dto = ProductDto.from(coffee);

        assertThat(dto.name()).isEqualTo("Kawa ziarnista 1kg");
        assertThat(dto.priceGrosze()).isEqualTo(6499);
        assertThat(dto.priceZl()).isEqualTo("64.99");
    }

    @Test
    @DisplayName("1. Test jednostkowy: grosze poniżej złotówki dostają zero wiodące (7 gr → \"0.07\")")
    void smallAmount() {
        assertThat(ProductDto.from(new Product("Próbka", 7)).priceZl()).isEqualTo("0.07");
    }
}
