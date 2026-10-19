package s00_start.start07_glossary;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Słowniczek Springa — pojęcia całego kursu, jednolite polskie nazwy i pojęcia, które łatwo pomylić
 *        (glossary = słowniczek; term = hasło, pojęcie; terminology = terminologia, czyli zestaw nazw używanych w dziedzinie)
 *
 * W SKRÓCIE:
 *   Spring ma własny język: bean, kontener, kontekst, starter, profil, proxy... Ta lekcja zbiera około stu pojęć w jednym
 *   miejscu: po angielsku (tak, jak widzisz je w kodzie i dokumentacji), po polsku (tak, jak nazywa je ten kurs),
 *   z krótką definicją i odesłaniem do lekcji. Słowniczek jest KODEM — rekord Term i lista w klasie SpringGlossary —
 *   więc można w nim szukać metodą i sprawdzać go testem. Do tego tabela terminologii kursu (jedna nazwa na jedno
 *   pojęcie) i sekcja o parach pojęć, które uczniowie najczęściej mylą.
 *
 * ANALOGIA: słownik na wyjeździe do obcego kraju.
 *   Nie uczysz się go na pamięć przed wyjazdem — zaglądasz, gdy usłyszysz nieznane słowo. Z czasem zaglądasz coraz
 *   rzadziej, bo słowa wchodzą w nawyk. Tabela terminologii to umowa z przewodnikiem: „zawsze mówimy dworzec, nigdy stacja”.
 *
 * JAK TO DZIAŁA:
 *   Term(english, polish, definition, lesson)    jedno hasło (rekord = niezmienna klasa danych)
 *   SpringGlossary.all()                         niezmienna lista, posortowana po english (komparator, nie HashMap)
 *   SpringGlossary.findByEnglish("bean")         dokładne wyszukiwanie po nazwie angielskiej (Optional)
 *   SpringGlossary.search("wstrzyk")             fragment nazwy angielskiej lub polskiej, bez względu na wielkość liter
 *   CourseTerminology.RULES                      tabela: nazwa angielska → polski odpowiednik + kalki, których unikamy
 *   MixedUpConcepts                              małe klasy do dowodów w sekcji 3 (bean, dubler testowy)
 *   Pole lesson w haśle: sNN_dzial/Klasa (ten kurs), sNN_dzial (dział bez nazw lekcji), tNN_pakiet/Klasa (JavaLearning).
 *
 * SŁÓWKA:
 *   glossary = słowniczek; term = hasło; lookup = wyszukiwanie; fragment = fragment; case-insensitive = bez względu na
 *   wielkość liter; comparator = komparator (obiekt porównujący); immutable = niezmienny; calque = kalka językowa
 *   (dosłowne, niezgrabne tłumaczenie); stereotype = stereotyp; test double = dubler testowy.
 *
 * ZOBACZ TEŻ: t00_start/Start02Glossary (słowniczek Javy — ten sam pomysł, inne pojęcia),
 *             s00_start/Start01HowToUse (jak korzystać z kursu), s01_core_ioc/Ioc01Container (bean, kontener, kontekst),
 *             s00_start/Start04TestingIntro (@SpringBootTest, MockMvc), s11_testing/Test02MockitoBeans (mock, stub, spy),
 *             t25_testing/Testing02TestDoubles (dublery testowe w czystej Javie).
 * </pre>
 */
public final class Start07Glossary {

    private Start07Glossary() {
    }

    // =================================================================================================
    // 1. JAK KORZYSTAĆ ZE SŁOWNICZKA
    // =================================================================================================
    //
    // Trzy sposoby (od najszybszego):
    //   a) Ctrl+F w pliku SpringGlossary.java — wpisz fragment angielskiej lub polskiej nazwy. Każde hasło to jeden blok
    //      t("angielska nazwa", "polska nazwa", "definicja", "odesłanie"), więc od razu widzisz całość.
    //   b) Ctrl+Shift+F (szukaj w całym projekcie) — np. „wstrzykiwanie zależności”: zobaczysz hasło słowniczka
    //      ORAZ lekcje, które tego pojęcia używają.
    //   c) Metoda: SpringGlossary.search("wstrzyk") zwraca listę haseł. Przydatne w teście albo w jshell.
    //
    // Pole lesson mówi, GDZIE pojęcie jest omówione naprawdę. Słowniczek daje 1-2 zdania, lekcja — pełne „dlaczego”.
    //
    // DOBRA PRAKTYKA: kiedy w lekcji trafisz na nieznane słowo, zajrzyj do słowniczka od razu, zamiast „jakoś zrozumieć
    //   z kontekstu”. Dlaczego? Pojęcia Springa budują się jedno na drugim (bean → kontener → kontekst → autokonfiguracja);
    //   jedna niezrozumiana nazwa potrafi zepsuć rozumienie kilku następnych lekcji.
    //
    // DOBRA PRAKTYKA: dopisuj własne hasła, gdy trafisz na pojęcie spoza listy — rekord Term jest niezmienny, ale lista
    //   to zwykły kod, więc dopisujesz kolejny wiersz t(...) (zachowaj porządek: lista i tak sortuje się sama).
    //   Dlaczego? Własne zdanie z własnymi słowami zapamiętujesz lepiej niż cudzą definicję.

    // =================================================================================================
    // 2. TABELA TERMINOLOGII KURSU
    // =================================================================================================
    //
    // W internecie to samo pojęcie ma wiele polskich nazw (a czasem żadnej dobrej). W tym kursie obowiązuje jedna:
    //
    //   angielska nazwa           w tym kursie                         NIE używamy
    //   ------------------------  -----------------------------------  ------------------------------
    //   bean                      bean (ziarno)                        fasola
    //   container                 kontener (IoC)                       —
    //   application context       kontekst aplikacji                   —
    //   dependency injection      wstrzykiwanie zależności             zastrzyk / iniekcja zależności
    //   inversion of control      odwrócenie sterowania                inwersja kontroli
    //   endpoint                  punkt końcowy (endpoint)             końcówka
    //   immutable                 niezmienny                           niemutowalny
    //   mutable                   zmienny                              mutowalny
    //   side effect               efekt uboczny                        —
    //   shadowing / hiding        przesłanianie / ukrywanie            cieniowanie
    //   property                  właściwość                           własność konfiguracji
    //   scope                     zasięg                               —
    //   proxy                     proxy (obiekt pośredniczący)         pełnomocnik
    //   test double               dubler testowy                       podwójny test
    //   slice test                test wycinkowy                       test plasterkowy
    //   autowiring                automatyczne wiązanie (autowiring)   autowajrowanie
    //   rollback                  wycofanie (rollback)                 —
    //   thread-safe               bezpieczny wątkowo                   wątkowo-bezpieczny
    //   callback                  wywołanie zwrotne                    —
    //
    // Ta sama tabela jest w kodzie (CourseTerminology.RULES), a test pilnuje, żeby słowniczek nie używał żadnej z „NIE
    // używamy”. Pozostałe pojęcia zostają po angielsku, gdy tak nazywa je cała branża (starter, stub, mock, endpoint w
    // kodzie) — wtedy przy pierwszym użyciu dajemy tłumaczenie w nawiasie.
    //
    // PUŁAPKA: tłumaczenie „na siłę” albo kalka z angielskiego, np. „niemutowalny” (od mutable). Dlaczego? Brzmi sztucznie,
    //   nikt tak nie mówi, a Ty zaczynasz powtarzać to na rozmowie o pracę. Po polsku mówimy „niezmienny”.
    //
    // DOBRA PRAKTYKA: ucz się ZAWSZE pary: angielska nazwa (z kodu i dokumentacji) + polska (z kursu). Dlaczego? Kod,
    //   komunikaty błędów i Stack Overflow są po angielsku — musisz je rozpoznać, a jednocześnie rozumieć wyjaśnienia.

    // =================================================================================================
    // 3. POJĘCIA, KTÓRE UCZEŃ MYLI
    // =================================================================================================
    //
    // PUŁAPKA: „bean to po prostu obiekt”. Dlaczego to błąd? Bean to obiekt zarządzany przez kontener: Spring go tworzy,
    //   wstrzykuje mu zależności, pilnuje cyklu życia (domyślnie jeden egzemplarz na cały kontener). Obiekt z new Counter()
    //   jest tylko obiektem — kontener o nim nie wie, więc nikt mu niczego nie wstrzyknie ani nie wykona @PostConstruct.
    //   Każdy bean jest obiektem, ale nie każdy obiekt jest beanem. (Test: beanVersusObject)
    //
    // PUŁAPKA: „kontener i kontekst to to samo”. Dlaczego to uproszczenie bywa groźne? Kontener (BeanFactory) tylko tworzy
    //   beany i łączy je zależnościami. Kontekst aplikacji (ApplicationContext) jest kontenerem PLUS kilka rzeczy: zna
    //   środowisko (Environment), publikuje zdarzenia, ładuje komunikaty. W praktyce pracujesz zawsze z kontekstem; słowo
    //   „kontener” oznacza rolę, a „kontekst” — konkretny obiekt, który ją pełni. (Test: containerVersusContext)
    //
    // PUŁAPKA: „starter to autokonfiguracja”. Dlaczego? Starter (np. spring-boot-starter-webmvc) jest PUSTYM artefaktem —
    //   zawiera tylko pom z listą zależności i żadnej klasy. Autokonfiguracja to klasy z warunkami (np. WebMvcAutoConfiguration),
    //   które leżą w osobnych jarach (spring-boot-webmvc), a starter je tylko przyciąga na ścieżkę klas. Dopiero ich
    //   obecność włącza konfigurację. (Test: starterVersusAutoConfiguration)
    //
    // PUŁAPKA: „mock, stub i spy to jedno”. Dlaczego warto je rozróżniać? Różnią się CELEM. Stub zwraca z góry ustalone
    //   odpowiedzi (po to, żeby test działał). Mock zapamiętuje wywołania, a test je weryfikuje (verify). Spy opakowuje
    //   PRAWDZIWY obiekt i wywołuje jego prawdziwe metody, chyba że jedną podmienisz. Mockito tworzy je podobnym kodem,
    //   więc pomyłka kosztuje: weryfikujesz stub, którego nie trzeba weryfikować, i test staje się kruchy.
    //   (Test: mockStubSpy)
    //
    // PUŁAPKA: „profil to środowisko”. Dlaczego? Profil to tylko ETYKIETA (dev, test, prod), po której Spring włącza beany i
    //   pliki konfiguracji. Environment (środowisko) to OBIEKT Springa, który zna aktywne profile ORAZ wszystkie źródła
    //   właściwości (plik, zmienne systemowe, argumenty). „Środowisko” w znaczeniu serwera (produkcja) to jeszcze coś trzeciego.
    //   (Test: profileVersusEnvironment)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Słowniczek = rekord Term + niezmienna, posortowana lista w SpringGlossary. Szukaj: Ctrl+F, Ctrl+Shift+F, search().
     *   • Pole lesson wskazuje lekcję: sNN_dzial/Klasa, sNN_dzial (dział bez nazw lekcji), tNN_pakiet/Klasa (JavaLearning).
     *   • Terminologia kursu: niezmienny (nie „niemutowalny”), wstrzykiwanie zależności, punkt końcowy, efekt uboczny.
     *   • Bean to obiekt zarządzany przez kontener; new poza kontenerem daje zwykły obiekt.
     *   • Kontener to rola (tworzy beany); kontekst aplikacji to kontener plus środowisko, zdarzenia, komunikaty.
     *   • Starter = pusty pom z zależnościami; autokonfiguracja = klasy z warunkami w osobnych jarach.
     *   • Stub zwraca, mock weryfikuje, spy opakowuje prawdziwy obiekt. Profil to etykieta, Environment — obiekt.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego lista haseł w SpringGlossary jest niezmienna i skąd wiemy, że jest posortowana?
     *   2. Co się stanie? Wywołasz SpringGlossary.all().add(new Term("X", "y", "z", "s00_start")).
     *   3. ZNAJDŹ BŁĄD: metoda szuka hasła tak: {@code term.english().contains(fragment.toLowerCase())}.
     *      Dla fragmentu "BEAN" nie znajduje hasła Bean. Dlaczego i jak to naprawić?
     *   4. Czym różni się bean od zwykłego obiektu? Co się stanie, gdy poprosisz kontekst o Counter, którego nie zarejestrowano?
     *   5. Starter spring-boot-starter-webmvc — z jakich klas się składa? Gdzie leży WebMvcAutoConfiguration?
     *   6. Kiedy użyjesz stuba, a kiedy mocka? Co zwróci spy na metodzie, której nie podmieniłeś?
     *   7. Co się stanie? Ustawisz w Environment aktywny profil "dev", ale nie dodasz żadnych właściwości. Czy
     *      env.getProperty("shop.name") coś zwróci?
     *   8. ZNAJDŹ BŁĄD: „Dodałem spring-boot-starter-webmvc, więc Boot zna już klasę WebMvcAutoConfiguration, bo starter
     *      ją zawiera.” Co jest nie tak?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start07GlossaryExercisesTest (rozwiązania: s00_start.solutions.start07_glossary)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć hasła, których POLSKA nazwa zawiera podany fragment — bez względu na wielkość liter
     * (porównuj po {@code toLowerCase(Locale.ROOT)} OBU stron). Zachowaj kolejność z listy wejściowej.
     * Przykład: fragment "BEAN" znajdzie hasło o polskiej nazwie „bean (ziarno)”.
     * Podpowiedź: strumień (stream), filter, toList.
     */
    static List<Term> exercise1(List<Term> terms, String fragment) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): pogrupuj hasła po DZIALE, czyli po tym, co stoi w polu lesson przed ukośnikiem
     * ({@code "s01_core_ioc/Ioc01Container"} → {@code "s01_core_ioc"}; odesłanie bez ukośnika jest już samą nazwą działu).
     * Wartością jest lista nazw ANGIELSKICH haseł z tego działu, posortowana alfabetycznie; klucze mają być
     * posortowane (użyj TreeMap — nigdy nie zakładamy kolejności z HashMap).
     * Podpowiedź: Collectors.groupingBy z fabryką mapy (TreeMap::new) albo pętla z computeIfAbsent.
     */
    static Map<String, List<String>> exercise2(List<Term> terms) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): „fiszka” — wylosuj jedno hasło z listy, DETERMINISTYCZNIE: ten sam seed (ziarno
     * generatora) musi dawać to samo hasło. Użyj {@code new Random(seed)} i {@code nextInt(terms.size())} — dokładnie
     * w tej kolejności, bo test porównuje wynik z takim samym losowaniem. Rzuć IllegalArgumentException dla pustej listy.
     * Podpowiedź: losowość z ziarnem to podstawa powtarzalnych testów (CLAUDE.md, sekcja 5).
     */
    static Term exercise3(List<Term> terms, long seed) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Lista pochodzi z List.of(...) i toList() — obie dają listę niezmienną, więc nikt nie dopisze hasła w trakcie
     *      działania programu i nie zepsuje wspólnych danych. Posortowanie ustala komparator (po nazwie angielskiej, bez
     *      względu na wielkość liter), a test porównuje sąsiednie hasła — nie polegamy na kolejności z HashMap.
     *   2. UnsupportedOperationException — niezmienna lista nie pozwala na add. Sprawdzone uruchomieniem
     *      (test immutableList).
     *   3. Fragment zamieniony na małe litery to "bean", ale nazwa Bean zostaje z wielką literą B, więc contains nie
     *      trafia. Trzeba zmienić wielkość liter OBU stron: term.english().toLowerCase(Locale.ROOT).contains(
     *      fragment.toLowerCase(Locale.ROOT)). Locale.ROOT, bo toLowerCase() bez argumentu zależy od języka systemu
     *      (przy tureckich ustawieniach „I” zmienia się w „ı” bez kropki i wyszukiwanie psuje się u części użytkowników).
     *      Błąd odtworzony w teście caseSensitiveSearchBug.
     *   4. Bean jest zarządzany przez kontener (zależności, cykl życia, zasięg); new Counter() tworzy zwykły obiekt,
     *      o którym kontener nie wie. Kontekst, w którym Counter nie zarejestrowano, nie ma takiego beana:
     *      getBeanProvider(Counter.class).getIfAvailable() zwraca null (a getBean rzuciłby
     *      NoSuchBeanDefinitionException). Sprawdzone uruchomieniem (test beanVersusObject).
     *   5. Z żadnej — jar startera zawiera tylko pom i metadane (pliki META-INF), zero klas. WebMvcAutoConfiguration leży
     *      w jarze spring-boot-webmvc, który starter przyciąga jako zależność. Sprawdzone uruchomieniem: test pokazuje
     *      lokalizację jaru, z którego załadowano klasę (test starterVersusAutoConfiguration).
     *   6. Stub — gdy zależność ma tylko podać dane, żeby test mógł działać (np. stan magazynu 0). Mock — gdy ważne jest,
     *      CZY i JAK zależność wywołano (verify). Spy na metodzie niepodmienionej wykonuje PRAWDZIWY kod opakowanego
     *      obiektu. Sprawdzone uruchomieniem (test mockStubSpy).
     *   7. Nie. Profil to tylko etykieta; Environment zwraca właściwość dopiero, gdy jakieś źródło ją zawiera, więc
     *      getProperty zwróci null. (Test profileVersusEnvironment: osobno profile i osobno właściwości.)
     *   8. Starter nie zawiera żadnych klas, tylko pom z zależnościami. Klasa WebMvcAutoConfiguration pochodzi z jaru
     *      spring-boot-webmvc, który starter wciąga tranzytywnie — „starter” i „autokonfiguracja” to dwie różne rzeczy.
     */
    // </editor-fold>
}
