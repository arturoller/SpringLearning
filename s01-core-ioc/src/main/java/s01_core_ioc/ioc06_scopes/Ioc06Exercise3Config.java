package s01_core_ioc.ioc06_scopes;

import org.springframework.context.annotation.Bean;

/**
 * Szkielet do ĆWICZENIA 3 lekcji Ioc06Scopes. Klasa NIE ma {@code @Configuration}, więc nie jest skanowana —
 * test rejestruje ją ręcznie (ApplicationContextRunner).
 */
public class Ioc06Exercise3Config {

    @Bean
    ShoppingCart cart() {
        // TODO: twoje rozwiązanie — dodaj nad metodą adnotację @Scope("prototype"), żeby każde getBean
        // dawało nowy koszyk. (Adnotacja @Scope z klasy ShoppingCart NIE działa na metodę @Bean — tu liczy się
        // adnotacja na metodzie.) Metoda ma zostać taka, jaka jest.
        return new ShoppingCart();
    }
}
