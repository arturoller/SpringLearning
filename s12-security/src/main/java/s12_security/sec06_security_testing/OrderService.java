package s12_security.sec06_security_testing;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * OrderService = zamówienia z zabezpieczeniem metody (jak w Sec02Authorization): swoje zamówienia widzi każdy,
 * cudze — tylko ADMIN. Testujemy go BEZ HTTP: {@code @WithMockUser} na metodzie testu + wywołanie serwisu.
 */
@Service
public class OrderService {

    private final Map<String, List<String>> ordersByOwner = Map.of(
            "anna", List.of("ZAM-001"),
            "piotr", List.of("ZAM-002", "ZAM-003"));

    @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    public List<String> ordersOf(String owner) {
        return ordersByOwner.getOrDefault(owner, List.of());
    }
}
