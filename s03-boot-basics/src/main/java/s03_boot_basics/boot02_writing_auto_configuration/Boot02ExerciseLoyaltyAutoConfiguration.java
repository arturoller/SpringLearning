package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.context.annotation.Bean;

/**
 * ĆWICZENIE 3 (trudniejsze): napisz autokonfigurację programu lojalnościowego. Wymagania (sprawdza je test):
 * <ol>
 *   <li>Bez właściwości {@code boot02.loyalty.enabled=true} — brak beana LoyaltyPolicy (i kontekst startuje bez błędu).</li>
 *   <li>Z {@code boot02.loyalty.enabled=true} — bean LoyaltyPolicy z rabatem z właściwości {@code boot02.loyalty.percent},
 *       a gdy jej brak — 5 procent.</li>
 *   <li>Gdy użytkownik ma własny bean LoyaltyPolicy — Twój się wycofuje.</li>
 * </ol>
 * Podpowiedź: warunek {@code @ConditionalOnProperty} na klasie, {@code @ConditionalOnMissingBean} na metodzie, a rabat
 * jako parametr metody: {@code @Value("${boot02.loyalty.percent:5}") int percent} (value = wartość; po dwukropku
 * wartość domyślna).
 *
 * <p>NIE dodawaj tu {@code @AutoConfiguration} ani {@code @Configuration}. Dlaczego? Ta klasa leży w pakiecie
 * skanowanym przez Boot02Application — z adnotacją trafiłaby do kontekstu lekcji jako zwykła konfiguracja. Test
 * traktuje ją jak autokonfigurację sam (AutoConfigurations.of). Rozwiązanie wzorcowe, w pakiecie solutions, ma już
 * pełny zestaw adnotacji — tak wygląda to w prawdziwej bibliotece.</p>
 */
public class Boot02ExerciseLoyaltyAutoConfiguration {

    @Bean
    LoyaltyPolicy loyaltyPolicy() {
        // TODO: twoje rozwiązanie (dodaj też warunki i parametr z rabatem)
        throw new UnsupportedOperationException("TODO");
    }
}
