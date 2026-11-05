package s11_testing.test02_mockito_beans;

/** Zewnętrzna usługa cen nie odpowiada. Na kod 503 zamienia go {@link ProductErrorAdvice}. */
public class PriceUnavailableException extends RuntimeException {

    public PriceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public PriceUnavailableException(String message) {
        super(message);
    }
}
