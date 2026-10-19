package s03_boot_basics.boot02_writing_auto_configuration;

/**
 * Druga klasa „biblioteki sklepu” — żegna klienta. Używana w ćwiczeniu 3 lekcji Boot02WritingAutoConfiguration:
 * bean tej klasy ma powstać tylko wtedy, gdy w kontekście jest już {@link ShopGreeter}.
 * Zwykła klasa Javy, BEZ adnotacji Springa (jak {@link ShopGreeter}).
 */
public class ShopFarewell {

    /** Np. „Do widzenia, Jan Kowalski! Zapraszamy ponownie.” */
    public String farewell(String customerName) {
        return "Do widzenia, " + customerName + "! Zapraszamy ponownie.";
    }
}
