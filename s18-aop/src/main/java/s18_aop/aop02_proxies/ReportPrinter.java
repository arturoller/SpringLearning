package s18_aop.aop02_proxies;

import org.springframework.stereotype.Component;

/**
 * Drukarka raportów — bean, którego nazwa („reportPrinter”) NIE pasuje do punktu przecięcia CountingAspect.
 * Dlatego Spring jej nie opakowuje: w kontekście jest zwykły obiekt (sekcja 1).
 */
@Component
public class ReportPrinter {

    public String print(String title) {
        return "== " + title + " ==";
    }
}
