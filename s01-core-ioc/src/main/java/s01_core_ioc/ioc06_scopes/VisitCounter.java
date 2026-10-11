package s01_core_ioc.ioc06_scopes;

import org.springframework.stereotype.Component;

/**
 * Licznik odwiedzin sklepu — singleton ze ZMIENNYM stanem (pole {@code count}). Wszyscy, którzy go dostaną, widzą
 * to samo pole. Do tego {@code count++} nie jest atomowe (atomic = niepodzielne): przy wielu wątkach część zwiększeń
 * zginie. Dlatego ta klasa jest przykładem tego, jak NIE robić.
 */
@Component
public class VisitCounter {

    private int count;

    /** Zwiększa licznik. Niebezpieczne przy wielu wątkach (odczyt, dodanie i zapis to trzy kroki). */
    public void increment() {
        count++;
    }

    public int count() {
        return count;
    }
}
