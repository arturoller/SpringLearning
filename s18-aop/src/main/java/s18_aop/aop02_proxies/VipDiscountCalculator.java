package s18_aop.aop02_proxies;

import org.springframework.context.annotation.ProxyType;
import org.springframework.context.annotation.Proxyable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Klasa FINAL z interfejsem i z {@code @Proxyable(ProxyType.INTERFACES)} (proxyable = „nadający się do proxy”;
 * nowość Spring Framework 7). Adnotacja zmienia typ proxy TYLKO dla tego beana: zamiast domyślnego w Boot CGLIB
 * (podklasa — niemożliwa dla klasy final) Spring robi proxy JDK z interfejsu DiscountPolicy. Bez niej aplikacja
 * by nie wystartowała (sekcja 5, FinalPriceService).
 */
@Service
@Proxyable(ProxyType.INTERFACES)
public final class VipDiscountCalculator implements DiscountPolicy {

    private static final Set<String> VIP_CUSTOMERS = Set.of("Jan Kowalski", "Zofia Krawczyk");

    @Override
    public BigDecimal discountPercent(String customerName) {
        return VIP_CUSTOMERS.contains(customerName) ? BigDecimal.TEN : BigDecimal.ZERO;
    }
}
