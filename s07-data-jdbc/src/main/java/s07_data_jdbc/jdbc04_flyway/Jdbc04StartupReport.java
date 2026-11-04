package s07_data_jdbc.jdbc04_flyway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Bean (ziarno), który w KONSTRUKTORZE czyta tabelę product. Pokazuje kolejność startu: Boot wykonuje migracje
 * Flyway, zanim utworzy beany zależne od DataSource/JdbcClient — dlatego tabela i dane już istnieją.
 */
@Component // component = komponent; Spring znajdzie go skanowaniem pakietu lekcji
@ConditionalOnProperty(name = "jdbc04.report", havingValue = "true") // conditional = warunkowy; włącza go profil jdbc04
public class Jdbc04StartupReport {

    private final int productsAtStartup;

    public Jdbc04StartupReport(JdbcClient jdbc) {
        this.productsAtStartup = jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single();
    }

    public int productsAtStartup() {
        return productsAtStartup;
    }
}
