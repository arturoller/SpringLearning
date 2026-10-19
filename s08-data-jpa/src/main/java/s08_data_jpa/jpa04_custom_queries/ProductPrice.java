package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * Projekcja jako REKORD DTO (data transfer object — obiekt do przenoszenia danych). Niezmienny, zwykły obiekt
 * Javy (nie encja, nie proxy) — świetny do zwracania z serwisu i do JSON-a. Spring Data wypełnia go przez
 * konstruktor kanoniczny: nazwy i kolejność składowych (name, price) wyznaczają kolumny zapytania (sekcja 4 lekcji).
 */
public record ProductPrice(String name, BigDecimal price) {
}
