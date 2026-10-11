package s02_configuration.cfg03_profiles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ustawienia bramki. Wartości dają pliki: application-cfg03.properties (baza) oraz application-cfg03-dev/prod.properties
 * (nadpisują bazę). Gdy żaden z plików nie jest wczytany (brak profilu), działają wartości domyślne z kodu.
 */
@Component
public class GatewaySettings {

    private final String url;
    private final Duration timeout;

    public GatewaySettings(@Value("${cfg03.gateway.url:brak}") String url,
                           @Value("${cfg03.gateway.timeout:1s}") Duration timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    public String url() {
        return url;
    }

    public Duration timeout() {
        return timeout;
    }
}
