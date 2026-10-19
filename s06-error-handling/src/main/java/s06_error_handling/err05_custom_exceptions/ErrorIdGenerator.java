package s06_error_handling.err05_custom_exceptions;

/**
 * Generator identyfikatorów błędów (correlation id = identyfikator korelacji: ten sam napis w logu i w odpowiedzi).
 * Interfejs, żeby test mógł podstawić stałą wartość (losowość jest zależnością, którą się wstrzykuje).
 */
public interface ErrorIdGenerator {

    String next();
}
