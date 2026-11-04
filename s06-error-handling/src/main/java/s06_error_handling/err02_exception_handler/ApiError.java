package s06_error_handling.err02_exception_handler;

/**
 * Własny, jednolity format błędu API sklepu (rekord = niezmienna klasa danych). Klient dostaje zawsze te same cztery pola:
 * {@code status} (kod HTTP), {@code code} (stabilny kod maszynowy, na którym klient może oprzeć warunek),
 * {@code message} (opis dla człowieka) i {@code path} (adres żądania).
 */
public record ApiError(int status, String code, String message, String path) {
}
