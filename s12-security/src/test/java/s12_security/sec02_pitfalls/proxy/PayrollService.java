package s12_security.sec02_pitfalls.proxy;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Serwis z pułapkami bezpieczeństwa metod (lekcja Sec02Authorization):
 * <ul>
 *   <li>{@code report()} woła {@code salaries()} na {@code this} — wywołanie z tej samej klasy omija proxy, więc
 *       {@code @PreAuthorize} na {@code salaries()} NIE jest sprawdzane (sekcja 5);</li>
 *   <li>{@code salariesWithPrefix()} ma {@code hasRole('ROLE_ADMIN')} — w wyrażeniach metod to działa (sekcja 2).</li>
 * </ul>
 */
@Service
public class PayrollService {

    @PreAuthorize("hasRole('ADMIN')")
    public String salaries() {
        return "Pensje: tajne";
    }

    /** Bez adnotacji — "przecież woła zabezpieczoną metodę". */
    public String report() {
        return "Raport: " + salaries();    // this.salaries() — z pominięciem proxy!
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String salariesWithPrefix() {
        return "Pensje: tajne";
    }
}
