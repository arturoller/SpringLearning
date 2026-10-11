package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * POPRAWNY cache produktów: ładowanie danych ("rozgrzewanie" cache) dzieje się w {@code @PostConstruct}, kiedy
 * zależność {@code catalog} jest już wstrzyknięta. To samo pole co w BrokenProductCache — inny moment użycia.
 */
@Component
public class ProductCache {

    @Autowired
    private ProductCatalog catalog;

    private final List<String> products = new ArrayList<>();

    @PostConstruct
    void warmUp() { // warm up = rozgrzewka: wypełnij cache przed pierwszym użyciem
        products.addAll(catalog.names());
    }

    public int size() {
        return products.size();
    }
}
