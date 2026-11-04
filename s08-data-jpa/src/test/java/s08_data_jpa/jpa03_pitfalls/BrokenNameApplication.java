package s08_data_jpa.jpa03_pitfalls;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa03_query_methods.Product;

/**
 * Osobna, celowo zepsuta aplikacja do sekcji 3 lekcji Jpa03QueryMethods. Leży w pakiecie TESTOWYM jpa03_pitfalls,
 * POZA jpa03_query_methods — inaczej klasa startowa lekcji zeskanowałaby wadliwe repozytorium i żadna lekcja by nie
 * wystartowała. Repozytoria: z tego pakietu (BrokenNameProductRepository); encja: Product lekcji ({@code @EntityScan}).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class BrokenNameApplication {
}
