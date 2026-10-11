package s02_configuration.cfg03_profiles;

import java.math.BigDecimal;

/** Bramka płatności (gateway = brama, pośrednik) — dwie implementacje wybierane profilem. */
public interface PaymentGateway {

    /** Krótka nazwa implementacji (do testów i logów). */
    String name();

    /** Obciąża kwotę i zwraca opis wyniku. */
    String charge(BigDecimal amount);
}
