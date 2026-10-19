package s21_capstone.cap01_shop_api.common;

import java.util.Map;

/**
 * Wspólny rodzic wyjątków DOMENY sklepu (domain = dziedzina — to, czym zajmuje się aplikacja). Klasa jest
 * {@code sealed} (zapieczętowana — t23_modern_java/Modern05RecordsSealedPatterns): dziedziczyć mogą tylko wymienione
 * podklasy, więc porada {@link ShopProblemAdvice} może użyć {@code switch} bez gałęzi „default” — kompilator sam
 * przypomni o nowym rodzaju błędu.
 *
 * <p>Wyjątek NIE zna HTTP (statusów, nagłówków). Mówi tylko „co się stało” ({@code code}, opis, szczegóły). Na status
 * zamienia go dopiero porada — dzięki temu ten sam serwis można wołać z zadania w tle albo z testu bez serwera.</p>
 */
public abstract sealed class ShopException extends RuntimeException
        permits NotFoundException, ConflictException, InvalidRequestException {

    private final String code;
    private final Map<String, Object> details;

    protected ShopException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);   // kopia niezmienna — nikt nie dopisze szczegółów po rzuceniu
    }

    /** Stały kod rodzaju błędu, np. ORDER_STATUS_CHANGE — klient może po nim rozpoznać sytuację. */
    public String code() {
        return code;
    }

    /** Dodatkowe pola dla klienta (trafią na wierzch ProblemDetail), np. {@code currentStatus}. */
    public Map<String, Object> details() {
        return details;
    }
}
