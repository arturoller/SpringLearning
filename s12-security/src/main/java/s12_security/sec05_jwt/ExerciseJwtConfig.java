package s12_security.sec05_jwt;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import java.time.Clock;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec05Jwt: dekoder tokenów BEZ tolerancji zegarów.
 */
public final class ExerciseJwtConfig {

    private ExerciseJwtConfig() {
    }

    /**
     * Ćwiczenie 4: dekoder HS256 z kluczem {@code key}, który sprawdza daty według {@code clock} i z ZEROWĄ tolerancją
     * (token jest nieważny już sekundę po {@code exp}). Reszta domyślnych walidatorów zostaje.
     */
    public static JwtDecoder decoder(SecretKey key, Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
