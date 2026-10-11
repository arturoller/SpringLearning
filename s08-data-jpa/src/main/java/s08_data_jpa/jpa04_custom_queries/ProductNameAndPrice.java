package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * ProductNameAndPrice — projekcja INTERFEJSOWA (interface projection): tylko gettery pól, które chcesz dostać.
 * Spring Data tworzy w locie obiekt-pośrednika implementującego ten interfejs, a zapytanie pobiera tylko te kolumny.
 */
public interface ProductNameAndPrice {

    String getName();

    BigDecimal getPrice();
}
