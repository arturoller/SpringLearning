package s04_web_rest.rest07_api_versioning;

import java.math.BigDecimal;
import java.util.List;

/**
 * VersionedModels = kształty odpowiedzi w różnych wersjach API — lekcja Rest07ApiVersioning.
 * Wersja 2.0 zmienia format ceny: zamiast liczby groszy — obiekt z kwotą i walutą. To ZMIANA ŁAMIĄCA (breaking
 * change): stary klient, który czyta "priceGrosze", przestałby działać — dlatego nowa wersja, a nie zmiana starej.
 */
public final class VersionedModels {

    private VersionedModels() {
    }

    /** ProductV1 = produkt w wersji 1.x: {"id":1,"name":"Kawa","priceGrosze":2999}. */
    public record ProductV1(long id, String name, int priceGrosze) {
    }

    /** Price = cena w wersji 2.0: {"amount":29.99,"currency":"PLN"}. */
    public record Price(BigDecimal amount, String currency) {
    }

    /** ProductV2 = produkt w wersji 2.0: {"id":1,"name":"Kawa","price":{"amount":29.99,"currency":"PLN"}}. */
    public record ProductV2(long id, String name, Price price) {
    }

    /** ProductPage = lista produktów od wersji 1.1: elementy + liczba wszystkich (nowe pole „total”). */
    public record ProductPage(List<ProductV1> items, int total) {
    }
}
