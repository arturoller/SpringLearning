package s08_data_jpa.solutions.jpa01_entities;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja dla testów rozwiązań lekcji Jpa01Entities. Skanuje TYLKO pakiet rozwiązań — encję ShopOrder
 * z rozwiązania, a nie szkielet z pakietu lekcji (dwie encje o nazwie ShopOrder w jednej bazie to konflikt).
 */
@SpringBootApplication
public class Jpa01SolutionsApplication {
}
