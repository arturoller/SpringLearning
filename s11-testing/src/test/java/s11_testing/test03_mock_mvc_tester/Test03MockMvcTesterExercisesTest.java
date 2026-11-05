package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test03MockMvcTester: TY piszesz test przy użyciu MockMvcTester (usuń linię
 * z UnsupportedOperationException; adnotacje {@code @WebMvcTest(ProductController.class)} i
 * {@code @Import(ProductService.class)} dodaj w nowej klasie). @Tag("cwiczenie") — uruchamiasz sam.
 * Rozwiązania wzorcowe: pakiet s11_testing.solutions.test03_mock_mvc_tester.
 */
@Tag("cwiczenie")
class Test03MockMvcTesterExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: GET /test03/products/2 → 200 i nazwa Herbata (hasStatusOk + extractingPath)")
    void exercise1() {
        // TODO: assertThat(mvc.get().uri(...)).hasStatusOk().bodyJson().extractingPath("$.name")...
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: GET /test03/products/99 → 404, typ application/problem+json i tytuł „Nie ma takiego produktu”")
    void exercise2() {
        // TODO: hasStatus(HttpStatus.NOT_FOUND).hasContentType("application/problem+json").bodyJson()...
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: POST poprawnego produktu → 201, Location …/test03/products/3 i JSON z nazwą (isLenientlyEqualTo)")
    void exercise3() {
        // TODO: mvc.post().uri(...).contentType(MediaType.APPLICATION_JSON).content("{...}")
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: GET /test03/boom → żądanie zakończone porażką; przyczyną jest IllegalStateException „Awaria serwisu”")
    void exercise4() {
        // TODO: hasFailed().failure().hasRootCauseInstanceOf(...).hasRootCauseMessage(...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 5: POST z pustą nazwą i ujemną ceną → 400, a w errors są OBA pola (name i price)")
    void exercise5() {
        // TODO: .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().hasPath("$.errors.name").hasPath("$.errors.price")
        throw new UnsupportedOperationException("TODO");
    }
}
