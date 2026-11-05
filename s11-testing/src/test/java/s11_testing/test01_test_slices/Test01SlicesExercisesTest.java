package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test01TestSlices: tym razem to TY piszesz test. W każdej metodzie jest opis, co sprawdzić.
 * Usuń linię {@code throw new UnsupportedOperationException("TODO")} i napisz test (adnotacje wycinka dodaj
 * na klasie — najlepiej w osobnej klasie na każdy rodzaj wycinka). @Tag("cwiczenie") — uruchamiasz sam (▶ obok metody).
 * Rozwiązania wzorcowe: pakiet s11_testing.solutions.test01_test_slices.
 */
@Tag("cwiczenie")
class Test01SlicesExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: zwykły test jednostkowy — rabat 0% nie zmienia ceny, rabat 100% daje 0,00")
    void exercise1() {
        // TODO: bez Springa: @ExtendWith(MockitoExtension.class), @Mock ProductRepository, Clock.fixed(...),
        //       new ProductService(...); given(repository.findById(1L)).willReturn(Optional.of(new Product(...)))
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: @WebMvcTest — GET /test01/products zwraca JSON z dwoma produktami z atrapy serwisu")
    void exercise2() {
        // TODO: @WebMvcTest(ProductController.class) + @MockitoBean ProductService; given(service.findAll()).willReturn(...).
        //       Produkt z bazy ma id nadawane przez bazę — w teście ustaw je przez ReflectionTestUtils.setField(product, "id", 1L).
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: @WebMvcTest — POST z pustą nazwą → 400, a serwis nie zostaje w ogóle zawołany")
    void exercise3() {
        // TODO: mockMvc.perform(post(...).contentType(JSON).content("{\"name\":\" \",\"price\":1}")) → status 400;
        //       potem verifyNoInteractions(service).
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: @JsonTest — JSON z dodatkowym polem czyta się na ProductRequest, a brak ceny daje null")
    void exercise4() {
        // TODO: @JsonTest + @Autowired JacksonTester<ProductRequest>; json.parseObject("{...}") dla dwóch dokumentów JSON.
        //       Sprawdź też: czy nieznane pole w JSON wywala test (w Boot nie — sprawdź uruchomieniem!).
        throw new UnsupportedOperationException("TODO");
    }
}
