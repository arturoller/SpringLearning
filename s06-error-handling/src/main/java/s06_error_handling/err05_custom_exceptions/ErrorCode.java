package s06_error_handling.err05_custom_exceptions;

/**
 * ErrorCode = kod błędu domenowego (domain = dziedzina, tu: sklep). To NIE jest kod HTTP — warstwa domeny nie zna
 * protokołu HTTP. Zamianę kodu błędu na status HTTP robi dopiero ShopExceptionAdvice (warstwa web).
 */
public enum ErrorCode {
    PRODUCT_NOT_FOUND,
    OUT_OF_STOCK,
    PAYMENT_UNAVAILABLE
}
