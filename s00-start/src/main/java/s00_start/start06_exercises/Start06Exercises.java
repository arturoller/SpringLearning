package s00_start.start06_exercises;

/**
 * <pre>
 * TEMAT: Jak działają ćwiczenia — szkielet z TODO, testy z @Tag("cwiczenie"), rozwiązania wzorcowe z @Tag("wzorzec")
 *        (exercise = ćwiczenie; skeleton = szkielet; tag = etykieta; solution = rozwiązanie; reference = wzorcowy)
 *
 * W SKRÓCIE:
 *   Każda lekcja kończy się ćwiczeniami. Szkielet (metoda z // TODO) leży w pakiecie lekcji i rzuca
 *   UnsupportedOperationException("TODO"). Testy ćwiczeń mają etykietę @Tag("cwiczenie") i są WYŁĄCZONE z ./mvnw test,
 *   żeby nierozwiązane ćwiczenia nie psuły budowania — uruchamiasz je sam, ▶ w IntelliJ. Rozwiązania wzorcowe leżą
 *   w osobnym pakiecie solutions, a ich testy (@Tag("wzorzec")) biegną zawsze: dowodzą, że ćwiczenie da się rozwiązać,
 *   a test jest poprawny.
 *
 * ANALOGIA: zeszyt ćwiczeń z odpowiedziami na końcu.
 *   Zadania (szkielety) są w środku zeszytu, odpowiedzi (solutions) — na końcu, w osobnym rozdziale, żeby nie kusiły.
 *   Nauczyciel (./mvnw test) sprawdza, czy klucz odpowiedzi jest poprawny, ale nie wystawia Ci oceny za niezrobione
 *   zadania. Sprawdzenie swoich zadań (testy z tagiem cwiczenie) zamawiasz sam, kiedy jesteś gotowy.
 *
 * JAK TO DZIAŁA:
 *   src/main/java/s00_start/start06_exercises/Start06Exercises.java        exercise1(...) { // TODO  throw ... }
 *   src/test/java/s00_start/start06_exercises/Start06ExercisesExercisesTest @Tag("cwiczenie") — domyślnie pomijany
 *   src/main/java/s00_start/solutions/start06_exercises/...Solutions.java  solution1(...) { gotowe rozwiązanie }
 *   src/test/java/s00_start/solutions/start06_exercises/...SolutionsTest   @Tag("wzorzec") — biegnie zawsze
 *   pom.xml (rodzic): właściwość excludedGroups = cwiczenie → Surefire (plugin uruchamiający testy) pomija ten tag
 *
 * SŁÓWKA:
 *   TODO = do zrobienia; unsupported operation = nieobsługiwana operacja; tag = etykieta testu; group = grupa testów
 *   (w Maven Surefire tagi JUnit nazywają się groups); exclude = wyklucz; surefire = plugin Mavena uruchamiający testy;
 *   solution = rozwiązanie; reference solution = rozwiązanie wzorcowe; hint = podpowiedź.
 *
 * ZOBACZ TEŻ: t00_start/Start01HowToUse (ćwiczenia w JavaLearning — tam sprawdzał je Check w main),
 *             s00_start/Start01HowToUse (budowa lekcji), s00_start/Start04TestingIntro (jak czytać testy ćwiczeń).
 * </pre>
 */
public final class Start06Exercises {

    private Start06Exercises() {
    }

    // =================================================================================================
    // 1. SZKIELET ĆWICZENIA
    // =================================================================================================
    //
    // Szkielet to metoda z gotowym podpisem (nazwa, parametry, typ wyniku), opisem w Javadoc i ciałem:
    //   // TODO: twoje rozwiązanie
    //   throw new UnsupportedOperationException("TODO");
    // Dzięki temu kod się kompiluje, a test ćwiczenia — dopóki go nie rozwiążesz — kończy się czerwonym ✘ z komunikatem
    // „UnsupportedOperationException: TODO”. Zastępujesz OBA wiersze swoim kodem.
    //
    // DOBRA PRAKTYKA: IntelliJ zbiera wszystkie komentarze TODO w oknie View → Tool Windows → TODO. Dlaczego warto?
    //   Widzisz listę niezrobionych ćwiczeń w całym kursie w jednym miejscu.

    // =================================================================================================
    // 2. TESTY ĆWICZEŃ — @Tag("cwiczenie")
    // =================================================================================================
    //
    // Klasa testów ćwiczeń ma adnotację @Tag("cwiczenie"). W rodzicu (pom.xml) jest właściwość
    //   <excludedGroups>cwiczenie</excludedGroups>
    // którą czyta Surefire — dlatego ./mvnw test POMIJA te testy, a budowanie jest zielone mimo nierozwiązanych ćwiczeń.
    // Jak uruchomić testy ćwiczeń:
    //   IntelliJ: ▶ obok klasy ...ExercisesTest albo obok jednej metody (IntelliJ nie stosuje excludedGroups).
    //   Linia poleceń: ./mvnw -pl s00-start test -Dgroups=cwiczenie -DexcludedGroups=none
    //     (-Dgroups = tylko ten tag; -DexcludedGroups=none nadpisuje wykluczenie z pom.xml nazwą nieistniejącego tagu)
    //
    // PUŁAPKA: samo ./mvnw test -Dgroups=cwiczenie uruchomi ZERO testów. Dlaczego? Surefire bierze testy z tagiem
    //   cwiczenie (groups), a potem i tak wyklucza tag cwiczenie (excludedGroups z pom.xml) — zostaje pusty zbiór.

    // =================================================================================================
    // 3. ROZWIĄZANIA WZORCOWE — pakiet solutions i @Tag("wzorzec")
    // =================================================================================================
    //
    // Rozwiązania leżą w s00_start.solutions.<lekcja> — to RODZEŃSTWO pakietu lekcji, nie jego podpakiet.
    // Dlaczego? Start06Application skanuje swój pakiet i podpakiety. Gdyby rozwiązanie kontrolera leżało w podpakiecie
    // lekcji, aplikacja miałaby DWA kontrolery z tym samym adresem /api/discount i nie wystartowałaby.
    // Rozwiązanie webowe ma więc własną klasę startową w pakiecie solutions (Start06SolutionsApplication) — test
    // @WebMvcTest rozwiązania szuka konfiguracji w górę drzewa pakietów i znajduje właśnie ją.
    //
    // DOBRA PRAKTYKA: do rozwiązania zaglądaj dopiero po własnej próbie — także nieudanej. Dlaczego? Wysiłek szukania
    //   rozwiązania (nawet błędnego) sprawia, że potem lepiej zapamiętujesz poprawne.
    //
    // PUŁAPKA: „skopiuję rozwiązanie do szkieletu i będzie zielono”. Będzie — ale nic się nie nauczysz. Testy ćwiczeń
    //   są dla Ciebie, nie dla oceny.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Szkielet: // TODO + throw new UnsupportedOperationException("TODO") — kompiluje się, test jest czerwony.
     *   • Testy ćwiczeń: @Tag("cwiczenie"), pomijane przez ./mvnw test (excludedGroups w pom.xml rodzica).
     *   • Uruchamianie: ▶ w IntelliJ albo ./mvnw -pl modul test -Dgroups=cwiczenie -DexcludedGroups=none.
     *   • Rozwiązania: pakiet sNN_dzial.solutions.lekcja (rodzeństwo lekcji), testy z @Tag("wzorzec") biegną zawsze.
     *   • Rozwiązanie z kontrolerem ma własną klasę @SpringBootApplication w pakiecie solutions.
     *   • Lista niezrobionych ćwiczeń: okno TODO w IntelliJ.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego szkielet rzuca UnsupportedOperationException, zamiast np. zwracać 0?
     *   2. Co się stanie? Uruchomisz ./mvnw -pl s00-start test -Dgroups=cwiczenie (bez -DexcludedGroups).
     *   3. Co się stanie? Przeniesiesz SolutionDiscountController do pakietu s00_start.start06_exercises.solutions
     *      (podpakiet lekcji) i uruchomisz Start06Application.
     *   4. ZNAJDŹ BŁĄD: klasa testów ćwiczeń nie ma @Tag("cwiczenie"). Co się stanie z ./mvnw test, zanim rozwiążesz ćwiczenia?
     *   5. Po co testy rozwiązań wzorcowych biegną przy każdym ./mvnw test?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start06ExercisesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): cena po rabacie w groszach, zaokrąglona do najbliższego grosza:
     * {@code (cena * (100 - procent) + 50) / 100}. Przykład: 10 000 gr i 15% → 8 500 gr; 999 gr i 10% → 899 gr.
     * Na razie załóż, że procent jest poprawny (sprawdzi go ćwiczenie 2).
     */
    static int exercise1(int priceGrosze, int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): sprawdź procent rabatu — dla wartości spoza 0..100 rzuć IllegalArgumentException
     * z komunikatem "Rabat musi być w zakresie 0..100: " + procent; dla poprawnej nic nie rób.
     * Potem wywołaj tę metodę na początku exercise1, żeby exercise1 też odrzucało złe procenty.
     */
    static void exercise2(int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /*
     * ĆWICZENIE 3 (trudniejsze): uzupełnij DiscountController.discount (plik DiscountController.java w tym pakiecie):
     *   GET /api/discount?price=10000 i percent=15 → 200 OK, {"price":10000,"percent":15,"discounted":8500};
     *   percent=150 → 400 Bad Request (dopisz @ExceptionHandler jak w PriceController z Start04TestingIntro).
     *   Test: Start06ExercisesExercisesTest — to @WebMvcTest, więc sprawdza tylko kontroler.
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo 0 mogłoby przypadkiem pasować do jakiejś asercji, a wyjątek zawsze daje czerwony test z jasnym komunikatem
     *      „TODO” — od razu wiesz, że ćwiczenie nie jest zrobione, a nie że jest zrobione źle.
     *   2. Nie uruchomi się żaden test: groups wybiera tag cwiczenie, a excludedGroups z pom.xml go wyklucza. Maven
     *      wypisze „Tests run: 0” i... BUILD SUCCESS. Zielone budowanie, choć nic nie zostało sprawdzone — dlatego
     *      zawsze patrz na liczbę testów, nie tylko na kolor. Sprawdzone uruchomieniem.
     *   3. Aplikacja nie wystartuje: oba kontrolery mapują GET /api/discount i Spring zgłasza błąd „Ambiguous mapping”
     *      (niejednoznaczne mapowanie). Dlatego rozwiązania leżą w pakiecie-rodzeństwie. Sprawdzone uruchomieniem.
     *   4. Testy ćwiczeń biegłyby przy każdym ./mvnw test i — dopóki ćwiczenia są nierozwiązane — kończyłyby się błędem,
     *      więc cały build byłby czerwony. Tag cwiczenie chroni budowanie.
     *   5. Dowodzą, że ćwiczenie jest rozwiązywalne, a jego test poprawny. Gdyby test ćwiczenia miał błąd, ten sam błąd
     *      zepsułby test wzorca — autor kursu zobaczy to od razu.
     */
    // </editor-fold>
}
