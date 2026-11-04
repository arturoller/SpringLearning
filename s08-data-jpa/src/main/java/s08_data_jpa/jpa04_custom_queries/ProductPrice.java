package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * Projekcja DTO jako REKORD (record — niezmienna klasa danych, t09_records/Records01Basics) lekcji Jpa04CustomQueries,
 * sekcja 3. DTO (Data Transfer Object — obiekt do przenoszenia danych) to zwykły obiekt, NIE encja: Hibernate go nie
 * śledzi, nie ma dirty checking, nie trafia do kontekstu utrwalania.
 */
public record ProductPrice(String name, BigDecimal price) {
}
