package s21_capstone.cap02_reservations.common;

import java.util.Map;

/**
 * Wyjątki domeny rezerwacji (sealed — zapieczętowana hierarchia, jak ShopException w Cap01ShopApi). Wyjątek mówi,
 * CO się stało ({@code code}, opis, szczegóły); status HTTP dobiera {@link BookingProblemAdvice}.
 */
public abstract sealed class BookingException extends RuntimeException
        permits NotFoundException, ConflictException, RuleViolationException {

    private final String code;
    private final Map<String, Object> details;

    protected BookingException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
