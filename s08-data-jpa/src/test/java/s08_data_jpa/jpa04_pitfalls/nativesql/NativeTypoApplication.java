package s08_data_jpa.jpa04_pitfalls.nativesql;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa04_custom_queries.Product;

/**
 * Druga celowo wadliwa aplikacja do sekcji 2 lekcji Jpa04CustomQueries: repozytorium z literówką w NATYWNYM SQL.
 * Osobny pakiet (rodzeństwo pakietu jpql), żeby obie aplikacje nie skanowały nawzajem swoich repozytoriów.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class NativeTypoApplication {
}
