package s20_modulith.mod01_modules;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import s20_modulith.mod01_modules.orders.OrderService;

/**
 * Klasa startowa lekcji Mod01Modules — korzeń monolitu modułowego. Spring Modulith traktuje KAŻDY bezpośredni
 * podpakiet tego pakietu jako moduł aplikacji: {@code customers}, {@code orders}, {@code products}.
 * Uruchomienie (▶ przy main): profil {@code mod01}, jedno zamówienie, wypisanie potwierdzenia i koniec.
 */
@SpringBootApplication
public class Mod01Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Mod01Application.class)
                .profiles("mod01").web(WebApplicationType.NONE).run(args)) {
            OrderService orders = context.getBean(OrderService.class);
            System.out.println("Złożono: " + orders.placeOrder(1, "KSI-001", 2));
        }
    }
}
