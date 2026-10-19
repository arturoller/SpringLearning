package s06_error_handling.err02_exception_handler;

/** Nasze własne, proste ciało błędu: kod (dla programu) i komunikat (dla człowieka). */
public record ErrorBody(String code, String message) {
}
