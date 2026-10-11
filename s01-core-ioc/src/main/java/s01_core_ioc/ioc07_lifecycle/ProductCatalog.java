package s01_core_ioc.ioc07_lifecycle;

import org.springframework.stereotype.Component;

import java.util.List;

/** ProductCatalog = katalog produktów sklepu (stała lista nazw). Zależność, której potrzebują inne beany. */
@Component
public class ProductCatalog {

    public List<String> names() {
        return List.of("Kawa", "Herbata", "Czekolada");
    }
}
