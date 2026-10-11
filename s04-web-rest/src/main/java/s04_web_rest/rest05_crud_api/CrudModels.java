package s04_web_rest.rest05_crud_api;

/**
 * CrudModels = rekordy API produktów lekcji Rest05CrudApi. Osobny rekord na każdy kierunek i rodzaj zmiany
 * (DTO — data transfer object, obiekt do przesyłania danych), zamiast jednego „do wszystkiego”.
 */
public final class CrudModels {

    private CrudModels() {
    }

    /** Product = produkt zwracany przez API (z id nadanym przez serwer). */
    public record Product(long id, String name, int priceGrosze) {
    }

    /**
     * ProductRequest = PEŁNE dane produktu: ciało POST (nowy) i PUT (zastąp całość). Bez id — id jest w adresie
     * albo nadaje je serwer. priceGrosze to int, więc jego brak w JSON-ie Jackson 3 sam odrzuci (400).
     */
    public record ProductRequest(String name, int priceGrosze) {
    }

    /**
     * ProductPatch = CZĘŚCIOWA zmiana (ciało PATCH). Typy obiektowe: null znaczy „tego pola nie zmieniaj”.
     * {"priceGrosze":2499} zmienia tylko cenę.
     */
    public record ProductPatch(String name, Integer priceGrosze) {
    }
}
