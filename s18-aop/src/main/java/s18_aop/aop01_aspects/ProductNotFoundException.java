package s18_aop.aop01_aspects;

/**
 * Wyjątek domenowy „nie ma takiego produktu” (do ćwiczenia 4). Niekontrolowany (RuntimeException), więc może
 * „wylecieć” z każdej metody bez deklaracji throws.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
