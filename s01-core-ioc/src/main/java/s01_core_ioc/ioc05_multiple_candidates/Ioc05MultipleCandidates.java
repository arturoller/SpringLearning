package s01_core_ioc.ioc05_multiple_candidates;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Kilka beanów tego samego typu — który wybrać?
 *        (candidate = kandydat, bean pasujący do zależności; primary = główny; qualifier = kwalifikator, „wskazówka
 *         którego beana chcę”; order = kolejność; ambiguity = niejednoznaczność)
 *
 * W SKRÓCIE:
 *   Interfejs NotificationSender ma trzy implementacje: EmailSender, SmsSender, PushSender — i wszystkie są beanami.
 *   Gdy klasa prosi w konstruktorze o JEDEN NotificationSender, kontener nie wie, którego dać, i rzuca
 *   NoUniqueBeanDefinitionException („nie ma jednego pasującego beana”). Wyjścia:
 *     • @Primary — jeden bean jest „domyślny”,
 *     • @Qualifier("nazwa") — konkretne miejsce wskazuje beana po nazwie,
 *     • List / Map — prosimy o WSZYSTKIE beany tego typu (lista w kolejności @Order, mapa nazwa → bean),
 *     • (awaryjnie) nazwa parametru konstruktora równa nazwie beana — działa, ale jest kruche.
 *
 * ANALOGIA: dyżurni w sklepie.
 *   Kierownik woła „niech ktoś zadzwoni do klienta!”. Jeśli dyżurują trzy osoby, nikt nie wie, kto ma to zrobić.
 *   @Primary to „domyślnie dzwoni Ewa”. @Qualifier to „akurat TĘ sprawę ma załatwić Marek”. List to „niech
 *   zadzwonią wszyscy, po kolei, według grafiku” (@Order).
 *
 * JAK TO DZIAŁA:
 *   Wstrzykiwanie parametru typu NotificationSender, kroki kontenera:
 *     1. zbiera wszystkich kandydatów tego typu (emailSender, smsSender, pushSender)
 *     2. jest @Qualifier? → wybiera beana o tej nazwie (kwalifikator wygrywa ze wszystkim)
 *     3. jest dokładnie jeden bean @Primary? → wybiera jego (dwa @Primary = błąd)
 *     4. awaryjnie: nazwa parametru równa nazwie beana? → wybiera go
 *     5. nic nie pasuje → NoUniqueBeanDefinitionException: "expected single matching bean but found 2: ..."
 *   Dla List<NotificationSender> i Map<String, NotificationSender> kontener nie wybiera — wstrzykuje wszystkich
 *   kandydatów (lista posortowana według @Order, mapa z nazwami beanów jako kluczami).
 *
 * SŁÓWKA:
 *   notification = powiadomienie; sender = nadawca, wysyłacz; unique = jedyny, jednoznaczny; primary = główny;
 *   qualifier = kwalifikator; order = kolejność; broadcast = rozgłoszenie (do wszystkich); fallback = rozwiązanie
 *   awaryjne; by name = po nazwie; ambiguous = niejednoznaczny.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie przez konstruktor), s01_core_ioc/Ioc03Stereotypes
 *             (nazwy beanów), s01_core_ioc/Ioc04BeanMethods (@Bean — jak zarejestrować kilka beanów jednego typu),
 *             t07_inheritance_polymorphism/Inherit04Interfaces (interfejs i wiele implementacji),
 *             t12_collections/Collections05Maps (mapa).
 * </pre>
 */
public final class Ioc05MultipleCandidates {

    private Ioc05MultipleCandidates() {
    }

    // =================================================================================================
    // 1. PROBLEM — dwóch kandydatów i brak wyboru
    // =================================================================================================
    //
    // Kontekst z SmsSender i PushSender oraz klasą, której konstruktor ma parametr (NotificationSender sender):
    //   UnsatisfiedDependencyException: ... No qualifying bean of type '...NotificationSender' available:
    //   expected single matching bean but found 2: smsSender,pushSender
    // Aplikacja nie wystartuje — i to dobrze: błąd widać od razu przy starcie, a nie dopiero w produkcji.
    // (Przykład jest w teście sekcji 1 — klasy demonstracyjne nie mają adnotacji, więc nie psują aplikacji lekcji.)
    //
    // PUŁAPKA: dodanie DRUGIEJ implementacji interfejsu może zepsuć istniejący kod, który prosił o jeden bean.
    //   Dlaczego? Do tej pory kandydat był jeden, więc wszystko działało „samo”; po dodaniu drugiego kontener
    //   musi wybierać, a Ty nic nie wskazałeś. Dlatego przy dodawaniu implementacji sprawdź, kto prosi o interfejs.

    // =================================================================================================
    // 2. @Primary — jeden bean jest domyślny
    // =================================================================================================
    //
    // @Component @Primary public class EmailSender implements NotificationSender { ... }
    //
    // OrderConfirmationService(NotificationSender sender) dostaje teraz EmailSender. Pozostałe beany nadal istnieją
    // i można je pobrać inaczej (kwalifikatorem, listą).
    //
    // PUŁAPKA: dwa beany @Primary tego samego typu to znowu błąd ("more than one 'primary' bean found among
    //   candidates"). Dlaczego? @Primary ma wskazać JEDNEGO faworyta; przy dwóch znowu nie ma rozstrzygnięcia.
    //
    // DOBRA PRAKTYKA: @Primary dawaj beanowi, który jest rozsądnym wyborem w 90% miejsc, a wyjątki obsłuż @Qualifier.
    //   Dlaczego? Większość kodu zostaje prosta (bez adnotacji), a miejsca „nietypowe” są jawnie oznaczone.

    // =================================================================================================
    // 3. @Qualifier — wskazanie beana po nazwie
    // =================================================================================================
    //
    // public DeliveryNotifier(@Qualifier("smsSender") NotificationSender sender) { ... }
    //
    // Kwalifikator wygrywa z @Primary: DeliveryNotifier dostaje SmsSender, choć @Primary ma EmailSender. Nazwa
    // "smsSender" to domyślna nazwa beana klasy SmsSender (patrz s01_core_ioc/Ioc03Stereotypes).
    //
    // PUŁAPKA: literówka w nazwie kwalifikatora ("smsSendr") wychodzi dopiero przy starcie, a zmiana nazwy klasy
    //   zmienia nazwę beana. Dlaczego? Nazwa to zwykły tekst — kompilator jej nie sprawdza. Gdy nazwa ma znaczenie,
    //   zdefiniuj ją jawnie (@Component("smsSender")) albo stwórz własną adnotację-kwalifikator.

    // =================================================================================================
    // 4. List i Map — prosimy o wszystkie beany tego typu
    // =================================================================================================
    //
    // BroadcastService(List<NotificationSender> senders, Map<String, NotificationSender> sendersByName)
    //   lista: [emailSender, smsSender, pushSender] — w kolejności @Order(1), @Order(2), @Order(3)
    //   mapa:  klucz = nazwa beana ("emailSender", "smsSender", "pushSender"), wartość = bean
    //
    // To wzorzec „strategii”: dodajesz nową implementację (np. WhatsAppSender), a BroadcastService nie wymaga żadnej
    // zmiany — kontener sam dołoży ją do listy i mapy. @Primary nie ma tu znaczenia (bierzemy wszystkich).
    //
    // PUŁAPKA: bez @Order kolejność w liście nie jest gwarantowana ani specjalnie zdefiniowana. Dlaczego? Wynika
    //   z kolejności rejestracji beanów, a ta zależy od skanowania. Gdy kolejność ma znaczenie, ustaw @Order jawnie
    //   (mniejsza liczba = wcześniej). W testach mapy porównuj po posortowaniu kluczy.

    // =================================================================================================
    // 5. Awaryjnie: nazwa parametru = nazwa beana
    // =================================================================================================
    //
    // public ByParameterNameConsumer(NotificationSender smsSender)  → dostaje bean "smsSender"
    // public RenamedParameterConsumer(NotificationSender sender)    → błąd: nazwa "sender" nie pasuje do żadnego beana
    //
    // Działa, bo parent pom Spring Boota kompiluje z opcją -parameters (nazwy parametrów trafiają do pliku .class
    // i Spring może je odczytać). Zweryfikowane testem: przy kontekście Sms + Push pierwsza klasa działa, druga nie.
    //
    // PUŁAPKA: to rozwiązanie jest kruche. Dlaczego? (1) zmiana nazwy parametru w IDE (refaktoryzacja) po cichu
    //   zmienia zachowanie programu; (2) bez opcji -parameters (inne narzędzie budowania) nazwy znikną;
    //   (3) @Primary ma pierwszeństwo przed nazwą — z EmailSender @Primary parametr "smsSender" i tak dostanie
    //   e-mail (sprawdzone testem). Zamiast tego użyj jawnego @Qualifier — widać go w kodzie i nie psuje się przy
    //   zmianie nazwy parametru.
    //
    // Jeszcze jedno narzędzie: @Fallback (awaryjny) — bean, który jest używany tylko wtedy, gdy nie ma innego
    // kandydata; odwrotność @Primary, przydatna w bibliotekach z domyślną implementacją.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dwóch kandydatów i brak wskazówki → NoUniqueBeanDefinitionException ("expected single matching bean but found 2").
     *   • Kolejność rozstrzygania: @Qualifier → jedyny @Primary → (awaryjnie) nazwa parametru → błąd.
     *   • @Primary: domyślny bean (tylko JEDEN na typ); @Qualifier("nazwa"): wskazanie w konkretnym miejscu.
     *   • List<Typ>: wszystkie beany, w kolejności @Order (mniejsza liczba = wcześniej); Map<String, Typ>: nazwa → bean.
     *   • Nazwa parametru = nazwa beana działa (parent pom Boota używa -parameters), ale jest kruche — wybierz @Qualifier.
     *   • Nowa implementacja interfejsu może zepsuć kod, który prosił o jeden bean tego typu.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki wyjątek i z jakim komunikatem zobaczysz, gdy dwa beany pasują do jednego parametru?
     *   2. Co się stanie? Dwa beany NotificationSender mają @Primary (EmailSender i SmsSender) i ktoś prosi o jeden.
     *   3. Co się stanie? DeliveryNotifier ma @Qualifier("smsSender"), a EmailSender jest @Primary. Którego dostanie?
     *   4. ZNAJDŹ BŁĄD: {@code public OrderConfirmationService(NotificationSender smsSender)} w kontekście, gdzie
     *      EmailSender jest @Primary — programista oczekuje SMS-a. Co naprawdę się wydarzy?
     *   5. Co się stanie? W kontekście Sms + Push (bez @Primary) klasa ma parametr {@code NotificationSender sender}.
     *   6. W jakiej kolejności będą kanały w {@code List<NotificationSender>} i co jest kluczem w mapie?
     *   7. Kiedy użyjesz @Primary, a kiedy @Qualifier?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc05MultipleCandidatesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): z mapy {@code Map<String, NotificationSender>} (nazwa beana → bean) zwróć posortowaną listę
     * nazw beanów. Podpowiedź: senders.keySet().stream().sorted().toList() — mapa ze Springa nie gwarantuje kolejności.
     */
    static List<String> exercise1(Map<String, NotificationSender> senders) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // ĆWICZENIE 2 (średnie): w klasie Ioc05Exercise2Config są dwie metody @Bean typu NotificationSender (e-mail i SMS).
    // Dodaj @Primary tak, żeby getBean(NotificationSender.class) zwracało wysyłacz e-mail. Podpowiedź: adnotację
    // stawiasz na metodzie @Bean (adnotacje z klasy EmailSender nie działają, gdy bean tworzy metoda!).
    // Test: Ioc05MultipleCandidatesExercisesTest.exercise2.
    //
    // ĆWICZENIE 3 (trudniejsze): w klasie Ioc05Exercise3Config uzupełnij metodę alertService: dodaj parametr
    // NotificationSender z @Qualifier("smsSender") i zwróć new AlertService(sender). Test rejestruje dwa wysyłacze
    // (smsSender i pushSender, bez @Primary) i sprawdza, że alert idzie SMS-em.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. UnsatisfiedDependencyException, a w nim NoUniqueBeanDefinitionException: "No qualifying bean of type
     *      '...NotificationSender' available: expected single matching bean but found 2: smsSender,pushSender".
     *      Sprawdzone uruchomieniem (test sekcji 1).
     *   2. Kontekst się nie uruchomi: "more than one 'primary' bean found among candidates: [smsSender, emailSender]".
     *      Sprawdzone testem (sekcja 2, twoPrimariesFail).
     *   3. SmsSender — @Qualifier wygrywa z @Primary. Sprawdzone testem (sekcja 3).
     *   4. Dostanie EmailSender, nie SMS: @Primary ma pierwszeństwo przed dopasowaniem po nazwie parametru. Sprawdzone
     *      testem (sekcja 5, nameFallbackIsFragile). Naprawa: @Qualifier("smsSender").
     *   5. Kontekst się nie uruchomi (NoUniqueBeanDefinitionException) — nazwa "sender" nie pasuje do żadnego beana.
     *      Sprawdzone testem (sekcja 5).
     *   6. EMAIL, SMS, PUSH (według @Order 1, 2, 3); kluczem jest nazwa beana: emailSender, smsSender, pushSender.
     *      Sprawdzone testem (sekcja 4).
     *   7. @Primary — gdy jeden bean jest sensownym wyborem prawie wszędzie; @Qualifier — gdy konkretne miejsce
     *      potrzebuje konkretnej implementacji (wygrywa z @Primary).
     */
    // </editor-fold>
}
