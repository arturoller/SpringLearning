package s07_data_jdbc.jdbc05_repository;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Pomocnik testów ćwiczeń: świeża baza H2 w pamięci (unikalna nazwa) ze schematem i danymi z migracji V1 i V2 lekcji
 * (skrypty wykonane bezpośrednio, bez Flyway i bez kontekstu Springa — szybko i bez współdzielenia stanu).
 */
public final class Jdbc05TestDatabase {

    private Jdbc05TestDatabase() {
    }

    public static EmbeddedDatabase create() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:db/migration/jdbc05/V1__create_product.sql")
                .addScript("classpath:db/migration/jdbc05/V2__seed_products.sql")
                .build();
    }
}
