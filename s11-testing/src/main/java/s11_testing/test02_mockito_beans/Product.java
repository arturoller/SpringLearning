package s11_testing.test02_mockito_beans;

import java.math.BigDecimal;
import java.time.Instant;

/** Typ WARTOŚCI (record — niezmienny, bez zachowań): nie ma sensu go mockować, tworzysz zwykłe {@code new}. */
public record Product(long id, String name, BigDecimal price, Instant updatedAt) {
}
