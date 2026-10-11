package s04_web_rest.rest02_request_data;

import java.util.List;

/**
 * RequestModels = modele zapytań i odpowiedzi lekcji Rest02RequestData. Rekordy zagnieżdżone w jednej klasie, żeby
 * było je widać razem. Każda odpowiedź „odbija” to, co kontroler odebrał — tak widać, co Spring zrobił z zapytaniem.
 */
public final class RequestModels {

    private RequestModels() {
    }

    /** ProductRef = odwołanie do produktu z adresu: kategoria i id. */
    public record ProductRef(String category, long id) {
    }

    /** SearchCriteria = kryteria wyszukiwania odebrane z parametrów zapytania ({@code ?name=...&tag=...}). */
    public record SearchCriteria(String name, Integer maxPrice, Integer minPrice, int page, int size, List<String> tags) {
    }

    /** ClientInfo = informacje o kliencie odebrane z nagłówków. */
    public record ClientInfo(String clientId, String language) {
    }

    /** OrderLine = pozycja zamówienia w ciele JSON: {"productId":1,"quantity":2}. */
    public record OrderLine(long productId, int quantity) {
    }

    /** NewOrder = nowe zamówienie w ciele JSON: {"customer":"Anna","lines":[...]} → rekord przez Jacksona. */
    public record NewOrder(String customer, List<OrderLine> lines) {
    }

    /** OrderSummary = podsumowanie przyjętego zamówienia. */
    public record OrderSummary(String customer, int lineCount, int totalQuantity) {
    }
}
