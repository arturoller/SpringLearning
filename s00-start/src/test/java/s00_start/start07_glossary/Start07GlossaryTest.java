package s00_start.start07_glossary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import s00_start.start07_glossary.MixedUpConcepts.Counter;
import s00_start.start07_glossary.MixedUpConcepts.RealStockChecker;
import s00_start.start07_glossary.MixedUpConcepts.SalesFacade;
import s00_start.start07_glossary.MixedUpConcepts.StockChecker;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy lekcji Start07Glossary. To zwykłe testy jednostkowe (bez @SpringBootTest): sprawdzamy dane słowniczka i
 * małe, prawdziwe kawałki Springa — cały kontekst aplikacji nie jest do tego potrzebny, a testy są dzięki temu szybkie.
 */
class Start07GlossaryTest {

    /** Dozwolone odesłania: sNN_dzial, sNN_dzial/Klasa albo tNN_pakiet/Klasa. */
    private static final Pattern LESSON_FORMAT =
            Pattern.compile("(s\\d\\d_[a-z0-9_]+(/[A-Z][A-Za-z0-9]+)?)|(t\\d\\d_[a-z0-9_]+/[A-Z][A-Za-z0-9]+)");

    // ----------------------------------------------------------------------------------------------
    // 1. Słowniczek jako dane — poprawność i wyszukiwanie
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("1. Słowniczek ma od 80 do 120 haseł")
    void size() {
        assertThat(SpringGlossary.all().size()).isBetween(80, 120);
    }

    @Test
    @DisplayName("1. Nazwy angielskie nie powtarzają się (bez względu na wielkość liter)")
    void noDuplicates() {
        Set<String> seen = new HashSet<>();
        for (Term term : SpringGlossary.all()) {
            assertThat(seen.add(term.english().toLowerCase(Locale.ROOT)))
                    .as("duplikat hasła: " + term.english()).isTrue();
        }
    }

    @Test
    @DisplayName("1. Każde hasło ma wszystkie pola wypełnione")
    void noBlankFields() {
        for (Term term : SpringGlossary.all()) {
            assertThat(term.english()).as("english").isNotBlank();
            assertThat(term.polish()).as("polish w haśle " + term.english()).isNotBlank();
            assertThat(term.definition()).as("definition w haśle " + term.english()).isNotBlank();
            assertThat(term.lesson()).as("lesson w haśle " + term.english()).isNotBlank();
        }
    }

    @Test
    @DisplayName("1. Lista jest posortowana alfabetycznie po nazwie angielskiej (komparator, nie HashMap)")
    void sortedByEnglish() {
        List<Term> all = SpringGlossary.all();
        for (int i = 1; i < all.size(); i++) {
            assertThat(all.get(i - 1).english().compareToIgnoreCase(all.get(i).english()))
                    .as(all.get(i - 1).english() + " powinno być przed " + all.get(i).english())
                    .isLessThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("1. Lista jest niezmienna — dopisanie hasła kończy się wyjątkiem")
    void immutableList() {
        assertThatThrownBy(() -> SpringGlossary.all().add(new Term("X", "y", "z", "s00_start")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("1. Każde odesłanie do lekcji ma poprawny format")
    void lessonReferenceFormat() {
        for (Term term : SpringGlossary.all()) {
            assertThat(term.lesson()).as("odesłanie w haśle " + term.english()).matches(LESSON_FORMAT);
        }
    }

    @Test
    @DisplayName("1. findByEnglish znajduje hasło bez względu na wielkość liter, a search — po fragmencie polskiej lub angielskiej nazwy")
    void lookup() {
        assertThat(SpringGlossary.findByEnglish("BEAN")).isPresent();
        assertThat(SpringGlossary.findByEnglish("nie ma takiego hasła")).isEmpty();
        assertThat(SpringGlossary.search("wstrzykiwanie")).extracting(Term::english)
                .contains("Dependency injection", "Constructor injection");
        assertThat(SpringGlossary.search("INJECTION")).extracting(Term::english)
                .contains("Dependency injection", "Constructor injection");
    }

    @Test
    @DisplayName("1. ZNAJDŹ BŁĄD: szukanie zmienia wielkość liter tylko fragmentu, więc \"BEAN\" nie znajduje hasła Bean")
    void caseSensitiveSearchBug() {
        String fragment = "BEAN";
        long buggy = SpringGlossary.all().stream()
                .filter(term -> term.english().contains(fragment.toLowerCase(Locale.ROOT)))
                .filter(term -> term.english().equals("Bean"))
                .count();
        assertThat(buggy).as("wersja z błędem nie trafia w \"Bean\"").isZero();
        assertThat(SpringGlossary.search(fragment)).extracting(Term::english).contains("Bean");
    }

    // ----------------------------------------------------------------------------------------------
    // 2. Tabela terminologii
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("2. Tabela terminologii ma zasadnicze reguły z CLAUDE.md (niezmienny, zmienny, efekt uboczny)")
    void terminologyTable() {
        Map<String, String> preferred = new java.util.HashMap<>();
        CourseTerminology.RULES.forEach(rule -> preferred.put(rule.english(), rule.preferred()));
        assertThat(preferred).containsEntry("immutable", "niezmienny")
                .containsEntry("mutable", "zmienny")
                .containsEntry("side effect", "efekt uboczny")
                .containsEntry("dependency injection", "wstrzykiwanie zależności");
        assertThat(CourseTerminology.RULES).extracting(CourseTerminology.Rule::english).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("2. Słowniczek nie używa żadnej z kalek zakazanych w tabeli terminologii")
    void glossaryAvoidsCalques() {
        for (CourseTerminology.Rule rule : CourseTerminology.RULES) {
            for (String banned : rule.avoid()) {
                for (Term term : SpringGlossary.all()) {
                    String text = (term.polish() + " " + term.definition()).toLowerCase(Locale.ROOT);
                    assertThat(text).as("hasło " + term.english() + " używa zakazanej formy: " + banned)
                            .doesNotContain(banned.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------------
    // 3. Pojęcia, które uczeń myli — dowody
    // ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("3. Bean to obiekt zarządzany przez kontener: Counter jest beanem dopiero po rejestracji, a kontekst daje zawsze ten sam egzemplarz")
    void beanVersusObject() {
        // AnnotationConfigApplicationContext = najprostszy kontekst; rejestrujemy klasy ręcznie, bez skanowania
        try (AnnotationConfigApplicationContext empty = new AnnotationConfigApplicationContext()) {
            empty.refresh();
            assertThat(empty.getBeanProvider(Counter.class).getIfAvailable())
                    .as("kontekst nie zna Countera, więc nie ma takiego beana").isNull();
            assertThatThrownBy(() -> empty.getBean(Counter.class))
                    .isInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class);
        }
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Counter.class)) {
            Counter first = context.getBean(Counter.class);
            Counter second = context.getBean(Counter.class);
            assertThat(first).as("domyślny zasięg singleton: ten sam egzemplarz").isSameAs(second);
            assertThat(new Counter()).as("new poza kontenerem: zwykły, inny obiekt").isNotSameAs(first);
        }
    }

    @Test
    @DisplayName("3. Kontekst aplikacji jest kontenerem PLUS środowisko i zdarzenia — samo BeanFactory ich nie ma")
    void containerVersusContext() {
        assertThat(org.springframework.beans.factory.BeanFactory.class).isAssignableFrom(ApplicationContext.class);
        assertThat(ApplicationContext.class).isAssignableTo(EnvironmentCapable.class);
        assertThat(ApplicationContext.class).isAssignableTo(ApplicationEventPublisher.class);

        DefaultListableBeanFactory bareContainer = new DefaultListableBeanFactory();
        assertThat(bareContainer).isNotInstanceOf(EnvironmentCapable.class);
        assertThat(bareContainer).isNotInstanceOf(ApplicationEventPublisher.class);
    }

    @Test
    @DisplayName("3. Starter nie zawiera klas — WebMvcAutoConfiguration leży w jarze spring-boot-webmvc i jest wpisana na listę autokonfiguracji")
    void starterVersusAutoConfiguration() throws Exception {
        Class<?> autoConfiguration = Class.forName("org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration");
        String jar = autoConfiguration.getProtectionDomain().getCodeSource().getLocation().toString();
        assertThat(jar).contains("spring-boot-webmvc-").doesNotContain("starter");

        // Boot czyta listę autokonfiguracji z plików META-INF/spring/...AutoConfiguration.imports (ImportCandidates)
        List<String> candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();
        assertThat(candidates).contains(autoConfiguration.getName());
    }

    @Test
    @DisplayName("3. Stub zwraca ustalone odpowiedzi, mock pamięta wywołania (verify), spy opakowuje prawdziwy obiekt")
    void mockStubSpy() {
        // STUB zapisany ręcznie (lambda): zwraca z góry ustalony stan magazynu, niczego nie weryfikujemy
        StockChecker stub = sku -> 0;
        assertThat(new SalesFacade(stub).canSell("ELE-001")).isFalse();

        // MOCK Mockito: interesuje nas CZY i JAK go wywołano
        StockChecker mockChecker = mock(StockChecker.class);
        when(mockChecker.stock("ELE-001")).thenReturn(7);
        assertThat(new SalesFacade(mockChecker).canSell("ELE-001")).isTrue();
        verify(mockChecker).stock("ELE-001");
        verify(mockChecker, never()).stock("ELE-002");
        assertThat(mockChecker.stock("SPO-002")).as("niezdefiniowana odpowiedź mocka to 0").isZero();

        // SPY: prawdziwy obiekt; niepodmienione metody wykonują prawdziwy kod
        RealStockChecker spyChecker = spy(new RealStockChecker());
        assertThat(spyChecker.stock("SPO-002")).as("prawdziwy kod: 300 sztuk czekolady").isEqualTo(300);
        doReturn(5).when(spyChecker).stock("ELE-002");
        assertThat(spyChecker.stock("ELE-002")).as("podmieniona odpowiedź").isEqualTo(5);
        assertThat(spyChecker.stock("ELE-001")).as("reszta nadal prawdziwa").isEqualTo(7);
        verify(spyChecker, org.mockito.Mockito.atLeastOnce()).stock(anyString());
    }

    @Test
    @DisplayName("3. Profil to etykieta, Environment to obiekt ze źródłami właściwości — jedno nie zastępuje drugiego")
    void profileVersusEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("dev");
        assertThat(environment.acceptsProfiles(Profiles.of("dev"))).isTrue();
        assertThat(environment.getProperty("shop.name")).as("profil sam nie dodaje żadnej właściwości").isNull();

        environment.getPropertySources().addFirst(new MapPropertySource("test-source", Map.of("shop.name", "Sklep Kursowy")));
        assertThat(environment.getProperty("shop.name")).isEqualTo("Sklep Kursowy");
        assertThat(environment.acceptsProfiles(Profiles.of("prod"))).as("inny profil nie jest aktywny").isFalse();
    }
}
