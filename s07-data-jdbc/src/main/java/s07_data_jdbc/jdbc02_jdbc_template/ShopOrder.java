package s07_data_jdbc.jdbc02_jdbc_template;

import java.time.LocalDate;
import java.util.List;

/** Zamówienie razem z pozycjami (nazwa ShopOrder, żeby nie mylić z SQL-owym słowem ORDER). */
public record ShopOrder(Long id, String customerName, LocalDate orderDate, List<OrderLine> lines) {
}
