package s06_error_handling.err05_custom_exceptions;

/**
 * Awaria infrastruktury (baza, zewnętrzna usługa) — to NIE jest błąd klienta, więc nie należy do ShopException.
 * Zawsze przekazujemy PRZYCZYNĘ ({@code cause}) — oryginalny wyjątek niższej warstwy, żeby w logu został cały łańcuch.
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
