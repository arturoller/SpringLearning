package s03_boot_basics.boot02_writing_auto_configuration;

/**
 * Klasa „biblioteki sklepu”: wita klienta. Zwykła klasa Javy — BEZ adnotacji Springa.
 *
 * <p>Tak wyglądają klasy w prawdziwych bibliotekach: biblioteka nie wie, czy ktoś użyje jej ze Springiem.
 * Bean z tej klasy tworzy dopiero autokonfiguracja ({@link ShopGreeterAutoConfiguration}), a ustawienia bierze
 * z {@link ShopGreeterProperties}.</p>
 */
public class ShopGreeter {

    private final String shopName;
    private final String greeting;

    public ShopGreeter(String shopName, String greeting) {
        this.shopName = shopName;
        this.greeting = greeting;
    }

    /** Np. „Dzień dobry, Jan Kowalski! Witamy w sklepie SpringLearning.” */
    public String greet(String customerName) {
        return greeting + ", " + customerName + "! Witamy w sklepie " + shopName + ".";
    }
}
