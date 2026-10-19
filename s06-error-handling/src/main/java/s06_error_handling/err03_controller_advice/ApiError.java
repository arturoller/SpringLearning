package s06_error_handling.err03_controller_advice;

/** Wspólne ciało błędu całego API: kod dla programu i komunikat dla człowieka. */
public record ApiError(String code, String message) {
}
