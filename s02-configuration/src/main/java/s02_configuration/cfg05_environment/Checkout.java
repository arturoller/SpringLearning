package s02_configuration.cfg05_environment;

/** Kasa sklepowa (checkout) — dwie wersje przełączane flagą {@code cfg05.feature.new-checkout}. */
public interface Checkout {

    /** Nazwa wersji kasy (do testów). */
    String version();
}
