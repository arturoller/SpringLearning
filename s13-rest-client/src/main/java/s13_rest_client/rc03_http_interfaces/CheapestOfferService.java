package s13_rest_client.rc03_http_interfaces;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

/**
 * CheapestOfferService = serwis sklepu: „najtańsza DOSTĘPNA oferta dostawcy w kategorii”.
 * Wstrzykuje SupplierApi jak każdy inny bean — nie wie, że pod spodem jest HTTP. W teście jednostkowym serwisu
 * możesz podać własną implementację interfejsu albo atrapę Mockito (s11_testing/Test02MockitoBeans).
 */
@Service
public class CheapestOfferService {

    private final SupplierApi supplier;

    public CheapestOfferService(SupplierApi supplier) {
        this.supplier = supplier;
    }

    public Optional<SupplierOffer> cheapestAvailable(String category) {
        return supplier.offers(category).stream()
                .filter(offer -> offer.available() > 0)
                .min(Comparator.comparing(SupplierOffer::price));
    }
}
