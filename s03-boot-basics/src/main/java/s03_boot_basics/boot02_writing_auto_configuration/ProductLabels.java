package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Kod APLIKACJI (nie biblioteki): serwis sklepu, który po prostu wstrzykuje PriceFormatter i PriceTagPrinter.
 * Nie wie, skąd się wzięły — dostarczyła je autokonfiguracja z pliku imports.
 */
@Service
public class ProductLabels {

    private final PriceFormatter formatter;
    private final PriceTagPrinter printer;

    public ProductLabels(PriceFormatter formatter, PriceTagPrinter printer) {
        this.formatter = formatter;
        this.printer = printer;
    }

    public String priceOf(BigDecimal price) {
        return formatter.format(price);
    }

    public String tagOf(String productName, BigDecimal price) {
        return printer.tag(productName, price);
    }
}
