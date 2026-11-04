package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Sekcja 1 i 5 lekcji Test01TestSlices: najtańszy test — ZWYKŁY JUnit + Mockito, bez Springa i bez kontekstu.
 * Serwis ma konstruktor, więc atrapę repozytorium i stały zegar podajemy ręcznie. Czas: milisekundy.
 * Struktura given / when / then (mając / gdy / wtedy) i {@code @Nested} (zagnieżdżone grupy testów).
 */
@ExtendWith(MockitoExtension.class) // extension = rozszerzenie JUnit; tworzy atrapy oznaczone @Mock — bez Springa
class Test01ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Mock
    ProductRepository repository;   // mock = atrapa (udawany obiekt)

    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);   // fixed = stały: zegar, który stoi w miejscu

    private ProductService service() {
        return new ProductService(repository, clock);   // zwykłe new — żadnego kontenera Springa
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Sekcja 1: data utworzenia pochodzi ze stałego zegara, a produkt trafia do repozytorium")
        void createUsesClock() {
            // given — repozytorium-atrapa „zapisuje” i zwraca ten sam obiekt
            given(repository.save(any(Product.class))).willAnswer(call -> call.getArgument(0));

            // when
            Product created = service().create("Kawa", new BigDecimal("24.90"));

            // then
            assertThat(created.getCreatedAt()).isEqualTo(NOW);
            assertThat(created.getName()).isEqualTo("Kawa");
            verify(repository).save(created);
        }
    }

    @Nested
    @DisplayName("priceAfterDiscount")
    class Discount {

        @Test
        @DisplayName("Sekcja 5: 10% rabatu od 24,90 daje 22,41")
        void discountIsApplied() {
            given(repository.findById(1L)).willReturn(Optional.of(new Product("Kawa", new BigDecimal("24.90"), NOW)));

            BigDecimal result = service().priceAfterDiscount(1L, 10);

            assertThat(result).isEqualByComparingTo("22.41");
        }

        @Test
        @DisplayName("Sekcja 5: rabat spoza 0..100 to IllegalArgumentException (i repozytorium nawet nie jest pytane)")
        void invalidPercent() {
            assertThatThrownBy(() -> service().priceAfterDiscount(1L, 101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0..100");
        }

        @Test
        @DisplayName("Sekcja 5: brak produktu to ProductNotFoundException")
        void missingProduct() {
            given(repository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service().priceAfterDiscount(99L, 10))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Nie ma produktu o numerze 99");
        }
    }
}
