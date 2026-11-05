package s11_testing.test01_test_slices;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/**
 * Pomocnicza klasa „testu” BEZ metod testowych — JUnit jej nie uruchamia. Służy Test01MissingServiceTest, który ładuje
 * jej kontekst ręcznie: {@code @WebMvcTest} bez atrapy serwisu = kontroler nie ma czego wstrzyknąć.
 */
@WebMvcTest(ProductController.class)
class WebMvcWithoutServiceCase {
}
