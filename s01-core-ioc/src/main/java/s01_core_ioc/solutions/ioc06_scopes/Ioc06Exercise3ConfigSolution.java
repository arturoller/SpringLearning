package s01_core_ioc.solutions.ioc06_scopes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import s01_core_ioc.ioc06_scopes.ShoppingCart;

/** Rozwiązanie ćwiczenia 3 lekcji Ioc06Scopes: jedna adnotacja {@code @Scope("prototype")} na metodzie. */
public class Ioc06Exercise3ConfigSolution {

    @Bean
    @Scope("prototype")
    ShoppingCart cart() {
        return new ShoppingCart();
    }
}
