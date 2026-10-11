package s02_configuration.cfg02_configuration_properties;

import java.math.BigDecimal;

/**
 * <pre>
 * TEMAT: @ConfigurationProperties — ustawienia w jednym, typowanym rekordzie
 *        (configuration properties = właściwości konfiguracyjne; binding = wiązanie, czyli przenoszenie wartości
 *         z kluczy do pól obiektu; prefix = przedrostek kluczy; type-safe = bezpieczne typowo)
 *
 * W SKRÓCIE:
 *   Zamiast sześciu osobnych @Value tworzysz jeden rekord z ustawieniami i adnotacją
 *   @ConfigurationProperties(prefix = "cfg02.shop.delivery"). Boot sam przenosi do niego wartości z plików:
 *   zamienia typy (Duration, DataSize, List, Map), obsługuje zagnieżdżone obiekty i wartości domyślne.
 *   Rekord jest niezmienny, wstrzykuje się go jak każdy inny bean i da się go utworzyć w teście zwykłym new.
 *
 * ANALOGIA: formularz zamówienia zamiast luźnych karteczek.
 *   PRZED: sześć karteczek (@Value) rozsianych po różnych klasach — nie wiadomo, które należą do siebie.
 *   PO: jeden formularz „Dostawa” z polami: przewoźnik, czas, magazyn (podformularz), lista miast, cennik.
 *   Wszystko w jednym miejscu, z nazwanymi polami i typami.
 *
 * JAK TO DZIAŁA:
 *   cfg02.shop.delivery.timeout=45s  ──►  Binder (spoiwo wiążące)  ──►  new DeliveryProperties(..., Duration.ofSeconds(45), ...)
 *     1. @ConfigurationPropertiesScan (albo @EnableConfigurationProperties) rejestruje rekord jako bean.
 *     2. Binder czyta wszystkie klucze zaczynające się od prefiksu (z luźnym dopasowaniem nazw — jak w lekcji Cfg01).
 *     3. Nazwy pól rekordu (maxParcels) odpowiadają kluczom w kebab-case (max-parcels).
 *     4. Brakujące klucze: wartość z @DefaultValue; bez niej null, 0, false (także dla List i Map — null!).
 *     5. Boot woła konstruktor rekordu. Wynik to zwykły bean w kontenerze.
 *
 * SŁÓWKA:
 *   record = rekord (niezmienna klasa danych); nested = zagnieżdżony; default value = wartość domyślna;
 *   binder = obiekt wiążący; unknown field = nieznane pole (klucz bez pola w rekordzie); scan = skanowanie;
 *   duration = czas trwania; data size = rozmiar danych (np. 512KB); metadata = metadane (opis kluczy dla IDE).
 *
 * ZOBACZ TEŻ: t09_records/Records02Constructors (rekordy z konstruktorami),
 *             s02_configuration/Cfg01Properties (@Value, priorytety — to samo źródło danych),
 *             s01_core_ioc/Ioc02ConstructorInjection (wstrzyknięcie rekordu do usługi),
 *             s02_configuration/Cfg03Profiles (inne wartości w innych profilach).
 * </pre>
 */
public final class Cfg02ConfigurationProperties {

    private Cfg02ConfigurationProperties() {
    }

    // =================================================================================================
    // 1. PROBLEM z @Value: ustawienia rozsypane po kodzie
    // =================================================================================================
    //
    // PRZED: DeliveryService z sześcioma parametrami @Value("${cfg02.shop.delivery....}") — długi konstruktor,
    //   prefiks powtórzony sześć razy, listy i mapy trudne do wyrażenia, a test musi stawiać Springa albo
    //   podawać sześć wartości po kolei.
    // PO: DeliveryService(DeliveryProperties properties) — jeden parametr, jeden typ.
    //
    // DOBRA PRAKTYKA: dla więcej niż 2–3 powiązanych ustawień używaj @ConfigurationProperties. Dlaczego?
    //   (1) typy: Duration, DataSize, List, Map i obiekty zagnieżdżone działają od razu; (2) grupowanie: ustawienia
    //   jednej funkcji leżą w jednym miejscu, z jednym prefiksem; (3) testowalność: rekord tworzysz przez new;
    //   (4) @Value zostaw dla pojedynczych, luźnych wartości.

    // =================================================================================================
    // 2. REKORD + @ConfigurationProperties — wiązanie przez konstruktor
    // =================================================================================================
    //
    // @ConfigurationProperties(prefix = "cfg02.shop.delivery")
    // public record DeliveryProperties(String carrier, @DefaultValue("5") int maxParcels, ...) { }
    //
    // Pola rekordu zapisane w camelCase (maxParcels) wiążą się z kluczami w kebab-case (max-parcels) — i odwrotnie.
    // Rekord jest niezmienny (immutable): wartości wchodzą raz, przez konstruktor, i nikt ich nie zmieni w trakcie
    // działania aplikacji. Test sekcji 2 sprawdza wartości wczytane z application-cfg02.properties.
    // Polskie litery w pliku .properties zapisujemy kodami unicode (patrz plik application-cfg02.properties) — powód: Cfg01Properties, sekcja 1.
    //
    // UWAGA (zmiana względem starych tutoriali): dawniej do wiązania przez konstruktor trzeba było dopisać
    // @ConstructorBinding. Przy jednym konstruktorze (a rekord ma jeden) nie trzeba — działa bez niej (test sekcji 2).
    // @ConstructorBinding potrzebna jest dziś tylko wtedy, gdy klasa ma KILKA konstruktorów.
    //
    // PUŁAPKA: stare przykłady używają klasy z getterami i setterami (@Component + @ConfigurationProperties).
    //   Taka zmienna klasa też działa, ale ustawienia da się zmienić w trakcie działania programu (setterem), a pola
    //   nie mogą być final. Dlaczego rekord jest lepszy? Ustawienia są stałe — niezmienność chroni przed błędem,
    //   w którym jedna klasa „po cichu” przestawia konfigurację dla wszystkich.

    // =================================================================================================
    // 3. OBIEKTY ZAGNIEŻDŻONE, LISTY I MAPY
    // =================================================================================================
    //
    //   cfg02.shop.delivery.warehouse.city=Łódź       → pole warehouse (rekord Warehouse), jego pole city
    //   cfg02.shop.delivery.cities=Warszawa,Kraków    → List<String> cities   (albo cities[0]=…, cities[1]=…)
    //   cfg02.shop.delivery.prices.warszawa=12.50     → Map<String, BigDecimal> prices   (klucz mapy: "warszawa")
    //
    // PUŁAPKA: w kluczach MAPY nie używaj polskich znaków (np. "Kraków").
    //   Dlaczego? Z kluczy mapy Boot usuwa wszystko, co nie jest literą ASCII, cyfrą ani myślnikiem:
    //   klucz prices.Kraków zamienia się w "Krakw". Sprawdzone testem (sekcja 3). Bezpieczne są małe litery ASCII
    //   i cyfry: "krakow". (Żeby zachować klucz dosłownie, zapisz go w nawiasach: prices.[Kraków] — ale po co sobie
    //   utrudniać.)

    // =================================================================================================
    // 4. KONWERSJA TYPÓW I @DefaultValue
    // =================================================================================================
    //
    //   timeout=45s → Duration;   max-label-size=2MB → DataSize (B, KB, MB, GB, TB);   prices=12.50 → BigDecimal.
    // Nieobecny klucz: @DefaultValue("5") int maxParcels daje 5. @DefaultValue przy polu-rekordzie
    // (@DefaultValue Warehouse warehouse) tworzy obiekt zagnieżdżony zawsze, nawet gdy w pliku nie ma żadnego jego klucza
    // — inaczej bez ani jednego klucza dostałbyś null. To samo przy List i Map: goły {@code @DefaultValue} daje PUSTĄ
    // kolekcję, a bez niego brakujący klucz daje null (sprawdzone testem — w Boot 4.1.1 kolekcje NIE są puste same z siebie).
    // Konwersja działa też bez pełnej aplikacji — Binder ma własną (test sekcji 4 używa lekkiego kontekstu).
    //
    // DOBRA PRAKTYKA: pieniądze jako BigDecimal, czas jako Duration, rozmiary jako DataSize — nie jako int "w sekundach"
    //   czy "w bajtach". Dlaczego? Nazwa pola nie musi zdradzać jednostki, a w pliku człowiek pisze "45s" albo "2MB",
    //   czyli to, co naprawdę ma na myśli.

    // =================================================================================================
    // 5. WŁĄCZANIE REKORDU: @ConfigurationPropertiesScan albo @EnableConfigurationProperties
    // =================================================================================================
    //
    // Sama adnotacja @ConfigurationProperties NIE tworzy beana (to tylko „etykieta” z prefiksem). Trzeba ją włączyć:
    //   a) @ConfigurationPropertiesScan na klasie startowej — skanuje pakiet i rejestruje wszystkie takie klasy
    //      (Cfg02Application, wzięte z tej lekcji);
    //   b) @EnableConfigurationProperties(DeliveryProperties.class) na dowolnej klasie konfiguracyjnej —
    //      jawnie, jedna klasa po drugiej (dobre w testach i bibliotekach).
    // Test sekcji 5 pokazuje: bez włączenia bean nie istnieje, z @EnableConfigurationProperties — istnieje.
    //
    // PUŁAPKA: dopisałeś @ConfigurationProperties do rekordu i dostajesz NoSuchBeanDefinitionException (brak beana).
    //   Dlaczego? Adnotacja sama niczego nie rejestruje — brakuje skanowania albo @EnableConfigurationProperties.

    // =================================================================================================
    // 6. BŁĘDY WIĄZANIA — głośny zły typ, CICHA literówka w kluczu
    // =================================================================================================
    //
    //   cfg02.shop.delivery.timeout=abc  → kontekst nie startuje (ConfigurationPropertiesBindException): to się widzi.
    //   cfg02.shop.delivery.carier=DPD   → literówka w KLUCZU: klucz jest po prostu ignorowany, a carrier zostaje null.
    //
    // PUŁAPKA: literówka w nazwie klucza nie daje żadnego błędu. Dlaczego? Domyślnie Boot ignoruje nieznane klucze
    //   z prefiksem (ignoreUnknownFields = true), bo ten sam prefiks mogą współdzielić różne klasy i pliki.
    //   Sprawdzone testem (sekcja 6). Dlatego warto sprawdzić wartość w teście, a podpowiedzi IDE ułatwiają unikanie literówek.
    //
    // NOTATKA o metadanych: dodając zależność spring-boot-configuration-processor (processor = procesor adnotacji),
    // dostajesz plik z opisem kluczy, a IntelliJ podpowiada je w application.properties i pokazuje dokumentację.
    // Tu jej nie dodajemy — to tylko wygoda edytora, nic nie zmienia w działaniu aplikacji.
    //
    // NOTATKA o walidacji: wartości można sprawdzać adnotacjami (@Validated, @Min, @NotBlank), ale to wymaga
    // startera walidacji — zajmiemy się tym w dziale s05 (walidacja).

    // =================================================================================================
    // 7. DLACZEGO TO LEPSZE NIŻ @Value — test bez Springa
    // =================================================================================================
    //
    // new DeliveryService(new DeliveryProperties("DHL", 3, Duration.ofSeconds(10), ..., List.of("Poznań"), Map.of(...)))
    // — usługę sprawdzasz zwykłym testem jednostkowym, bez kontekstu Springa, w ułamku sekundy. Test sekcji 7.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @ConfigurationProperties(prefix = "…") na rekordzie: Boot wiąże klucze z polami przez konstruktor.
     *   • Nazwy: pole maxParcels ↔ klucz max-parcels (kebab-case w plikach).
     *   • Zagnieżdżony obiekt: klucz.obiekt.pole; lista: a,b,c lub [0]; mapa: klucz.mapa.klucz-mapy=wartość.
     *   • Typy: Duration ("45s"), DataSize ("512KB"), BigDecimal, List, Map — konwersja automatyczna.
     *   • @DefaultValue("…") przy polu rekordu: wartość, gdy klucza brak; brak → null/0/false (także List/Map);
     *     goły @DefaultValue przy List/Map/rekordzie zagnieżdżonym daje pustą kolekcję / obiekt z wartościami domyślnymi.
     *   • Trzeba włączyć: @ConfigurationPropertiesScan (skanowanie) lub @EnableConfigurationProperties(Klasa.class).
     *   • Zły typ wartości = błąd przy starcie; literówka w kluczu = cicho ignorowana.
     *   • Jeden rekord zamiast wielu @Value: typy, grupowanie, testowalność.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @Value od @ConfigurationProperties i kiedy warto wybrać które?
     *   2. Co się stanie? W pliku wpisujesz cfg02.shop.delivery.timeout=abc.
     *   3. Co się stanie? W pliku wpisujesz cfg02.shop.delivery.carier=DPD (brak litery r), a rekord ma pole carrier.
     *   4. ZNAJDŹ BŁĄD: rekord ma @ConfigurationProperties("cfg02.exercise.discount"), ale nie ma ani skanowania,
     *      ani @EnableConfigurationProperties — dlaczego getBean(Discount.class) nie działa?
     *   5. Jaką wartość ma maxParcels, gdy klucza max-parcels nie ma w żadnym pliku, a pole ma @DefaultValue("5")?
     *      A jaką, gdy pole ma zwykły {@code int} bez @DefaultValue?
     *   6. Dlaczego rekord pasuje do ustawień lepiej niż klasa z setterami?
     *   7. ZNAJDŹ BŁĄD: w mapie cen używasz klucza {@code cfg02.shop.delivery.prices.Kraków=15.00} — co jest ryzykowne?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cfg02ConfigurationPropertiesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zrób z {@code Discount} ustawienia z prefiksem {@code cfg02.exercise.discount}.
     * Dopisz {@code @ConfigurationProperties(...)} nad rekordem oraz wartości domyślne: percent = 10, enabled = true.
     * Podpowiedź: {@code @DefaultValue("10")} stoi przed typem pola rekordu.
     */
    record Discount(int percent, boolean enabled) {
        // TODO: twoje rozwiązanie (adnotacje nad rekordem i przy jego polach)
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć ustawienia (tekst {@code "klucz=wartość"}), po których {@link DeliveryProperties}
     * będzie miało: przewoźnika "DPD", maxParcels 2, listę miast [Poznań, Wrocław] oraz ceny poznan=11.00
     * i wroclaw=13.50. Podpowiedź: prefiks to {@code cfg02.shop.delivery}; lista po przecinku; klucze mapy ASCII.
     */
    static String[] exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): policz koszt wysyłki = cena z mapy {@code prices} (po {@code cityKey}) razy liczba paczek.
     * Gdy paczek jest więcej niż {@code maxParcels} albo brak ceny dla miasta — rzuć {@link IllegalArgumentException}.
     * Podpowiedź: {@code properties.prices().get(...)}, BigDecimal.valueOf(parcels), multiply.
     */
    static BigDecimal exercise3(DeliveryProperties properties, String cityKey, int parcels) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Value wstrzykuje JEDNĄ wartość (dobre dla pojedynczego ustawienia). @ConfigurationProperties wiąże całą grupę
     *      kluczy z jednym obiektem (typy, zagnieżdżenia, listy, mapy, wartości domyślne, test bez Springa) — wybierz je
     *      od 2–3 powiązanych ustawień.
     *   2. Kontekst nie wystartuje: wyjątek wiązania (ConfigurationPropertiesBindException) z informacją, że "abc" nie
     *      jest poprawnym czasem trwania. Sprawdzone testem (sekcja 6).
     *   3. Nic się nie stanie — klucz jest cicho ignorowany, a carrier zostaje null. Sprawdzone testem (sekcja 6).
     *   4. Sama adnotacja @ConfigurationProperties nie rejestruje beana. Dodaj @ConfigurationPropertiesScan na klasie
     *      startowej albo @EnableConfigurationProperties(Discount.class). Sprawdzone testem (sekcja 5).
     *   5. Z @DefaultValue("5") — 5. Bez niego — 0 (domyślna wartość typu int; dla String byłoby null, dla boolean false).
     *      Sprawdzone testem (sekcja 4).
     *   6. Rekord jest niezmienny: ustawienia wchodzą raz przez konstruktor i nikt ich nie przestawi w trakcie działania;
     *      pola są final, a klasa krótka (bez getterów i setterów).
     *   7. Polskie znaki: Boot usuwa z klucza mapy znaki spoza ASCII, więc klucz "Kraków" staje się "Krakw" i kod
     *      szukający "Kraków" niczego nie znajdzie. Bezpiecznie: małe litery ASCII, np. "krakow".
     *      Sprawdzone testem (sekcja 3).
     */
    // </editor-fold>
}
