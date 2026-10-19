package s03_boot_basics.boot03_spring_application;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Katalog produktów sklepu wczytywany przy starcie aplikacji (przez {@link CatalogLoaderRunner}).
 * Źródłem jest stała lista 14 produktów (ten sam „świat” co w całym kursie) — bez bazy danych i bez plików.
 */
@Component
public class ProductCatalog {

    /** Wszystkie produkty sklepu: „SKU nazwa”. */
    public static final List<String> ALL_PRODUCTS = List.of(
            "ELE-001 Laptop Pro 14", "ELE-002 Smartfon X", "ELE-003 Słuchawki BT", "ELE-004 Monitor 27 cali",
            "SPO-001 Kawa ziarnista 1kg", "SPO-002 Czekolada gorzka", "SPO-003 Oliwa z oliwek",
            "KSI-001 Czysty kod", "KSI-002 Java. Podstawy", "KSI-003 Wzorce projektowe",
            "ODZ-001 Kurtka zimowa", "ODZ-002 T-shirt bawełniany", "DOM-001 Ekspres do kawy", "DOM-002 Lampka biurkowa");

    private volatile List<String> loaded = List.of();

    /** Wczytuje pierwsze {@code limit} produktów (nie więcej niż jest w katalogu). */
    public void load(int limit) {
        loaded = ALL_PRODUCTS.subList(0, Math.max(0, Math.min(limit, ALL_PRODUCTS.size())));
    }

    public List<String> loaded() {
        return loaded;
    }
}
