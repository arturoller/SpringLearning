package s18_aop.aop02_proxies;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rezerwacja towaru — klasa BEZ interfejsu, więc zawsze dostaje proxy CGLIB.
 * reserveAll woła reserve przez {@code this} — to PUŁAPKA wywołania z tej samej klasy (sekcja 4).
 */
@Service
public class StockService {

    public String reserve(String sku, int quantity) {
        return "zarezerwowano " + quantity + " x " + sku;
    }

    /** PUŁAPKA: this.reserve(...) — wywołanie na „gołym” obiekcie, z pominięciem proxy i porad. */
    public List<String> reserveAll(List<String> skus) {
        List<String> results = new ArrayList<>();
        for (String sku : skus) {
            results.add(this.reserve(sku, 1));
        }
        return results;
    }

    /** Czy „this” wewnątrz metody to proxy? (Nie — to obiekt docelowy, schowany ZA proxy.) */
    public boolean thisIsProxy() {
        return AopUtils.isAopProxy(this);
    }
}
