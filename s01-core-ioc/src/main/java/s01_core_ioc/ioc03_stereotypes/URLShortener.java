package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Component;

/**
 * Nazwa klasy zaczyna się od DWÓCH wielkich liter, więc domyślna nazwa beana to "URLShortener"
 * (a nie "uRLShortener"). Skracacz adresów — przykład z życia (url = adres internetowy).
 */
@Component
public class URLShortener {

    public String shorten(String url) {
        return "sklep.pl/" + Integer.toHexString(url.hashCode() & 0xffff);
    }
}
