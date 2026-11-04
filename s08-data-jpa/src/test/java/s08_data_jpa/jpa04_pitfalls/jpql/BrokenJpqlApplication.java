package s08_data_jpa.jpa04_pitfalls.jpql;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa04_custom_queries.Product;

/**
 * Osobna, celowo zepsuta aplikacja do sekcji 1 lekcji Jpa04CustomQueries. Leży w pakiecie TESTOWYM jpa04_pitfalls,
 * POZA jpa04_custom_queries — inaczej klasa startowa lekcji zeskanowałaby wadliwe repozytorium i żaden test lekcji
 * by nie wystartował. Repozytorium: z tego pakietu; encja: Product lekcji ({@code @EntityScan} — skanuj encje).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class BrokenJpqlApplication {
}
