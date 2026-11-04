package s09_jpa_relations.rel05_n_plus_one;

import java.math.BigDecimal;

/** Wynik ĆWICZENIA 4 lekcji Rel05NPlusOne: klient, liczba jego zamówień i łączna wartość pozycji. */
public record CustomerSpending(String customerName, long orderCount, BigDecimal total) {
}
