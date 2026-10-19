package s01_core_ioc.ioc06_scopes;

import org.springframework.context.annotation.Bean;

/**
 * Demonstracja z sekcji 2 lekcji Ioc06Scopes: metoda {@code @Bean} BEZ {@code @Scope}. Mimo że klasa ShoppingCart ma
 * {@code @Scope("prototype")}, bean z tej metody jest singletonem — dla metody {@code @Bean} liczy się adnotacja na
 * metodzie. Klasa celowo NIE ma {@code @Configuration}, więc Ioc06Application jej nie skanuje — test lekcji rejestruje
 * ją ręcznie (ApplicationContextRunner). To osobna klasa, a nie szkielet ćwiczenia 3, żeby test lekcji nie zmieniał
 * wyniku po rozwiązaniu ćwiczenia.
 */
public class SingletonCartConfig {

    @Bean
    ShoppingCart cart() {
        return new ShoppingCart();
    }
}
