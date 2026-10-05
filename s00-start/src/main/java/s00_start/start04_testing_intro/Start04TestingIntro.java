package s00_start.start04_testing_intro;

import java.util.List;

/**
 * <pre>
 * TEMAT: Wprowadzenie do testów w Springu — jak czytać „checkery” kursu: JUnit, AssertJ, @SpringBootTest, MockMvc,
 *        @WebMvcTest i @MockitoBean
 *        (test = test; assertion = asercja, sprawdzenie; mock = atrapa; slice = wycinek aplikacji; checker = sprawdzacz)
 *
 * W SKRÓCIE:
 *   Testy w kursie są „wynikiem” lekcji, więc musisz umieć je czytać od pierwszego dnia. Są trzy rodzaje:
 *   (1) test jednostkowy — zwykła Java, bez Springa: new PriceCalculator() i sprawdzenie wyniku (najszybszy);
 *   (2) @SpringBootTest — uruchamia CAŁY kontekst aplikacji, a MockMvc wysyła do niego zapytania HTTP „na niby”;
 *   (3) @WebMvcTest — uruchamia tylko WYCINEK webowy (kontrolery); serwisy trzeba podać jako atrapy (@MockitoBean).
 *   Zasada wyboru: najwęższy test, który wystarcza do sprawdzenia danej rzeczy.
 *
 * ANALOGIA: kontrola samochodu.
 *   Test jednostkowy to sprawdzenie jednej części na stole (sama żarówka). @SpringBootTest to jazda próbna całym autem.
 *   @WebMvcTest to sprawdzenie samej deski rozdzielczej podłączonej do symulatora silnika (atrapy) — szybciej niż jazda,
 *   dokładniej niż sama żarówka.
 *
 * JAK TO DZIAŁA:
 *   @Test void grossWithVat() {                       ← JUnit: metoda testowa; uruchamiasz ▶ obok niej
 *       // given (dane):  PriceCalculator calculator = new PriceCalculator();
 *       // when (akcja):  int gross = calculator.gross(10_000, 23);
 *       // then (wynik):  assertThat(gross).isEqualTo(12_300);   ← AssertJ: „upewnij się, że ... jest równe ...”
 *   }
 *   zielony ✔ = asercje spełnione; czerwony ✘ = różnica, a komunikat pokazuje: expected (oczekiwane) i actual (faktyczne)
 *
 * SŁÓWKA:
 *   unit test = test jednostkowy; given/when/then = mając/gdy/wtedy (układ testu); expected = oczekiwane;
 *   actual = faktyczne; mock = atrapa (udawany obiekt); stub = zaślepka (atrapa z ustaloną odpowiedzią);
 *   verify = sprawdź (czy metoda atrapy została wywołana); slice test = test wycinka; perform = wykonaj (zapytanie);
 *   status = status odpowiedzi HTTP; json path = ścieżka w JSON-ie ($.gross = pole gross w głównym obiekcie).
 *
 * ZOBACZ TEŻ: t32_junit_mockito/JUnit01Basics (JUnit od podstaw), t32_junit_mockito/JUnit03AssertJ (asercje AssertJ),
 *             t32_junit_mockito/JUnit04Mockito (atrapy Mockito), t25_testing/Testing02TestDoubles (rodzaje atrap),
 *             s00_start/Start03FirstApplication (aplikacja, którą testujemy podobnie), s00_start/Start06Exercises.
 * </pre>
 */
public final class Start04TestingIntro {

    private Start04TestingIntro() {
    }

    // =================================================================================================
    // 1. TEST JEDNOSTKOWY — bez Springa (Start04TestingIntroUnitTest)
    // =================================================================================================
    //
    // PriceCalculator to zwykła klasa z adnotacją @Service. Adnotacja nie przeszkadza: w teście jednostkowym tworzymy
    // obiekt przez new i wołamy metodę. Spring w ogóle nie startuje — test trwa milisekundy.
    // Układ given/when/then (mając / gdy / wtedy) porządkuje test: dane → akcja → sprawdzenie.
    //
    // DOBRA PRAKTYKA: logikę (obliczenia, reguły) trzymaj w serwisach i testuj jednostkowo. Dlaczego? Takie testy są
    //   najszybsze i najdokładniej wskazują błąd — nie trzeba startować serwera ani kontekstu.

    // =================================================================================================
    // 2. ASERCJE AssertJ — czytanie i komunikaty błędów
    // =================================================================================================
    //
    //   assertThat(x).isEqualTo(5)                     x równe 5
    //   assertThat(lista).containsExactly(a, b)        lista ma dokładnie a, b — w tej kolejności
    //   assertThat(tekst).contains("VAT")              tekst zawiera „VAT”
    //   assertThatThrownBy(() -> kod)                  kod rzuca wyjątek...
    //       .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("VAT")   ...tego typu, z takim komunikatem
    //
    // PUŁAPKA: assertThat(x) BEZ dalszej metody (np. isEqualTo) niczego nie sprawdza — test zawsze przejdzie.
    //   Dlaczego? assertThat tylko „bierze wartość do ręki”; sprawdzenie robi dopiero kolejna metoda.

    // =================================================================================================
    // 3. @SpringBootTest + MockMvc — cała aplikacja (Start04TestingIntroSpringBootTest)
    // =================================================================================================
    //
    // @SpringBootTest uruchamia pełny kontekst Start04Application: skanowanie, wszystkie beany, autokonfiguracja.
    // @AutoConfigureMockMvc dodaje bean MockMvc, który wysyła zapytania HTTP prosto do DispatcherServlet — bez portu
    // i bez sieci. Czytanie:
    //   mockMvc.perform(get("/api/price").param("net", "10000").param("vat", "23"))   ← wyślij GET z parametrami
    //          .andExpect(status().isOk())                                           ← oczekuj 200 OK
    //          .andExpect(jsonPath("$.gross").value(12300));                         ← pole gross w JSON = 12300
    //
    // PUŁAPKA: w Spring Boot 4 MockMvc i @WebMvcTest są w osobnej zależności spring-boot-starter-webmvc-test i w pakiecie
    //   org.springframework.boot.webmvc.test.autoconfigure. Dlaczego to ważne? Importy z tutoriali do Boot 3
    //   (org.springframework.boot.test.autoconfigure.web.servlet...) w Boot 4 się nie skompilują.

    // =================================================================================================
    // 4. @WebMvcTest + @MockitoBean — tylko warstwa web (Start04TestingIntroWebMvcTest)
    // =================================================================================================
    //
    // @WebMvcTest(PriceController.class) tworzy wycinek kontekstu: kontroler + infrastrukturę MVC (JSON, MockMvc),
    // ale NIE tworzy serwisów. Kontroler potrzebuje PriceCalculator, więc podajemy atrapę:
    //   @MockitoBean PriceCalculator calculator;                      ← atrapa Mockito jako bean w kontekście testu
    //   given(calculator.gross(10_000, 23)).willReturn(99_999);       ← „gdy ktoś zapyta o to, odpowiedz 99999”
    //   ... perform(...) → jsonPath("$.gross").value(99999)           ← kontroler oddał to, co dał serwis
    //   verify(calculator).gross(10_000, 23);                         ← i rzeczywiście zapytał serwis
    // Taki test sprawdza TYLKO kontroler: mapowanie adresu, parametry, JSON, obsługę błędów.
    //
    // PUŁAPKA: w starszych tutorialach zobaczysz @MockBean. W Spring Boot 4 tej adnotacji już NIE MA (była przestarzała
    //   od Boot 3.4). Dlaczego? Zastąpiło ją @MockitoBean ze Spring Framework (pakiet
    //   org.springframework.test.context.bean.override.mockito) — działa tak samo, a jest częścią samego Spring Test.

    // =================================================================================================
    // 5. KTÓRY TEST WYBRAĆ?
    // =================================================================================================
    //
    //   co sprawdzasz                                → test
    //   obliczenia, reguły w serwisie                → jednostkowy (new + asercje)
    //   kontroler: adres, parametry, JSON, kody HTTP → @WebMvcTest + @MockitoBean
    //   czy wszystko razem działa (beany, konfiguracja) → @SpringBootTest (+ MockMvc)
    //
    // DOBRA PRAKTYKA: wybieraj najwęższy wystarczający test. Dlaczego? Każdy start kontekstu kosztuje czas; przy setkach
    //   testów różnica to sekundy kontra minuty. Spring na szczęście zapamiętuje (cache) kontekst między testami
    //   o tej samej konfiguracji.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Test — metoda testowa (JUnit); @DisplayName("...") — opis po polsku; @Tag("...") — etykieta (np. cwiczenie).
     *   • given / when / then — dane, akcja, sprawdzenie.
     *   • AssertJ: assertThat(x).isEqualTo(...), containsExactly(...), assertThatThrownBy(() -> ...).isInstanceOf(...).
     *   • @SpringBootTest — cały kontekst; @AutoConfigureMockMvc + MockMvc — zapytania HTTP bez sieci.
     *   • @WebMvcTest(Kontroler.class) — tylko warstwa web; zależności kontrolera jako @MockitoBean.
     *   • Mockito: given(atrapa.metoda(...)).willReturn(...); verify(atrapa).metoda(...).
     *   • Boot 4: @MockitoBean zamiast @MockBean; MockMvc/@WebMvcTest w spring-boot-starter-webmvc-test.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się test jednostkowy od @SpringBootTest? Kiedy wybrać który?
     *   2. Co się stanie? new PriceCalculator().gross(999, 23) — jaki wynik i dlaczego?
     *   3. Co się stanie? Usuniesz z Start04TestingIntroWebMvcTest pole z @MockitoBean i uruchomisz test.
     *   4. ZNAJDŹ BŁĄD: {@code assertThat(calculator.gross(10_000, 23));} — test zawsze jest zielony. Dlaczego?
     *   5. Co sprawdza verify(calculator).gross(10_000, 23) i po co je dodawać?
     *   6. Dlaczego w @WebMvcTest pole gross w odpowiedzi to 99999, a nie 12300?
     *   7. ZNAJDŹ BŁĄD: test w Boot 4 ma {@code import org.springframework.boot.test.mock.mockito.MockBean;} i się
     *      nie kompiluje. Co poprawić?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start04TestingIntroExercisesTest (przeczytaj je — to też nauka czytania testów)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy stawka VAT jest dozwolona (0, 5, 8 albo 23)? Podpowiedź: PriceCalculator.ALLOWED_VAT.
     */
    static boolean exercise1(int vatPercent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): cena brutto po rabacie. Najpierw rabat od ceny netto (zaokrąglenie jak w PriceCalculator:
     * {@code (netto * (100 - rabat) + 50) / 100}), potem VAT przez new PriceCalculator().gross(...).
     * Przykład: 10 000 gr, VAT 23%, rabat 10% → netto 9 000 → brutto 11 070. Rabat spoza 0..100 → IllegalArgumentException.
     */
    static int exercise2(int netGrosze, int vatPercent, int discountPercent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): suma cen brutto koszyka — każda pozycja liczona OSOBNO (z własnym zaokrągleniem),
     * potem zsumowana. Przykład: [999, 999] przy 23% → 1229 + 1229 = 2458. Uwaga: [1, 1, 1] przy 23% → 1 + 1 + 1 = 3,
     * choć brutto z sumy netto (3 gr) to 4 gr — zaokrąglenie każdej pozycji osobno daje inny wynik niż zaokrąglenie sumy.
     * Pusta lista → 0. Podpowiedź: stream().mapToInt(net -> calculator.gross(net, vat)).sum()
     * (t16_streams/Streams08PrimitiveStreams).
     */
    static int exercise3(List<Integer> netPricesGrosze, int vatPercent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jednostkowy tworzy obiekt przez new i nie startuje Springa — szybki, sprawdza logikę jednej klasy.
     *      @SpringBootTest startuje cały kontekst — wolniejszy, sprawdza, czy beany i konfiguracja działają razem.
     *      Logika → jednostkowy; „czy aplikacja działa jako całość” → @SpringBootTest.
     *   2. 1229. 999 * 123 = 122 877; + 50 = 122 927; dzielenie całkowite przez 100 = 1229 (czyli 1228,77 zł/100
     *      zaokrąglone do najbliższego grosza). Sprawdzone testem w Start04TestingIntroUnitTest.
     *   3. Kontekst testu się nie uruchomi: PriceController wymaga beana PriceCalculator, a @WebMvcTest nie tworzy
     *      serwisów. Błąd: „No qualifying bean of type ...PriceCalculator available” (brak pasującego beana).
     *      Sprawdzone uruchomieniem.
     *   4. Brak metody sprawdzającej po assertThat(...) — nic nie jest porównywane. Poprawnie np. .isEqualTo(12_300).
     *   5. Sprawdza, że kontroler wywołał serwis dokładnie z argumentami (10 000, 23). Po co, skoro JSON i tak się zgadza?
     *      Bo verify sprawdza ZACHOWANIE (rozmowę z serwisem), a nie tylko wynik. Gdy coś się zepsuje, komunikat Mockito
     *      pokazuje wprost oczekiwane i faktyczne wywołanie, np. zamienione argumenty.
     *   6. Bo w @WebMvcTest serwis jest atrapą, która zwraca to, co jej kazaliśmy (99 999). Test sprawdza kontroler,
     *      nie obliczenia — liczby „z kosmosu” pokazują to wyraźnie.
     *   7. W Boot 4 @MockBean nie istnieje. Użyj {@code import org.springframework.test.context.bean.override.mockito.MockitoBean;}
     *      i adnotacji @MockitoBean.
     */
    // </editor-fold>
}
