package s08_data_jpa.jpa04_custom_queries;

import java.math.BigDecimal;

/**
 * ProductSummary — projekcja KLASOWA (DTO, data transfer object = obiekt do przenoszenia danych) jako rekord.
 * Niezmienna, bez pośredników, z gotowym equals/hashCode/toString — idealna do odczytu danych „do wyświetlenia”.
 */
public record ProductSummary(String name, BigDecimal price) {
}
