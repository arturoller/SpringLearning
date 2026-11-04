package s11_testing.test03_mock_mvc_tester;

import java.math.BigDecimal;

/** DTO wyjściowe: to, co widzi klient. Rekord — test może zamienić JSON z powrotem na ten typ (convertTo). */
public record ProductResponse(long id, String name, BigDecimal price) {
}
