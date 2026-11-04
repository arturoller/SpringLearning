package s08_data_jpa.jpa04_custom_queries;

import org.springframework.beans.factory.annotation.Value;

/**
 * Projekcja „OTWARTA” (open projection) lekcji Jpa04CustomQueries, sekcja 3 — PUŁAPKA. Wartość gettera liczy
 * wyrażenie SpEL z {@code @Value} (value = wartość), a {@code target} to cała encja. Spring Data nie wie, których pól
 * wyrażenie potrzebuje, więc wczytuje CAŁĄ encję (sprawdzone testem: pełny SELECT i 3 wczytane encje).
 */
public interface ProductLabel {

    @Value("#{target.name + ' (' + target.price + ' zł)'}")
    String getLabel();
}
