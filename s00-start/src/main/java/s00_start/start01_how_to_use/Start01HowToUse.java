package s00_start.start01_how_to_use;

import java.time.LocalDate;

/**
 * <pre>
 * TEMAT: Jak korzystać z kursu SpringLearning — budowa lekcji, tagi, nazwy, testy jako wynik, powtórki
 *        (course = kurs; lesson = lekcja; tag = znacznik; test = test, czyli program sprawdzający inny program)
 *
 * W SKRÓCIE:
 *   Ten kurs to kontynuacja JavaLearning i działa tak samo: czytasz lekcję, PRZEWIDUJESZ wynik, uruchamiasz, porównujesz,
 *   odpowiadasz na pytania kontrolne, robisz ćwiczenia i powtarzasz po 1, 3, 7, 14 i 30 dniach.
 *   Jedna różnica: w JavaLearning „wynikiem” był komentarz // WYNIK: pod wywołaniem w main. Tutaj wynikiem jest TEST —
 *   zielony pasek w IntelliJ oznacza „kod robi dokładnie to, co opisuje lekcja”.
 *
 * ANALOGIA: książka kucharska z kontrolą jakości.
 *   Przepis (lekcja) mówi, co zrobić i dlaczego. Na końcu przepisu jest zdjęcie gotowego dania (test) — porównujesz
 *   swoje danie ze zdjęciem. Jeśli zmienisz przepis (kod), od razu widzisz, czy danie nadal wygląda jak na zdjęciu.
 *
 * JAK TO DZIAŁA:
 *   moduł (Maven)        s00-start                         ← jeden dział = jeden moduł, nazwa z myślnikami
 *   pakiet działu        s00_start                         ← ta sama nazwa z podkreślnikami
 *   pakiet lekcji        s00_start.start01_how_to_use      ← jedna lekcja = jeden podpakiet
 *   klasa lekcji         Start01HowToUse                   ← nagłówek z tagami, sekcje, ŚCIĄGA, pytania, ćwiczenia
 *   test lekcji          Start01HowToUseTest               ← w src/test/java, w TYM SAMYM pakiecie co lekcja
 *
 * SŁÓWKA:
 *   module = moduł (podprojekt Mavena); package = pakiet; lesson = lekcja; exercise = ćwiczenie; solution = rozwiązanie;
 *   review = powtórka; spaced repetition = powtórki rozłożone w czasie; assertion = asercja (sprawdzenie w teście);
 *   green bar = zielony pasek (wszystkie testy przeszły); red bar = czerwony pasek (co najmniej jeden test nie przeszedł).
 *
 * ZOBACZ TEŻ: t00_start/Start01HowToUse (to samo w JavaLearning), t00_start/Start03LearningPath (metoda nauki),
 *             t00_start/Start04ReviewTracker (dziennik powtórek), s00_start/Start04TestingIntro (jak czytać testy),
 *             s00_start/Start06Exercises (jak działają ćwiczenia).
 * </pre>
 */
public final class Start01HowToUse {

    private Start01HowToUse() {
        // lekcja-dokument: nie tworzymy obiektów tej klasy
    }

    // =================================================================================================
    // 1. BUDOWA LEKCJI — tagi
    // =================================================================================================
    //
    // Każda lekcja ma ten sam układ, a każdy element jest oznaczony TAGIEM (znacznikiem) z dwukropkiem.
    // Tagi są te same co w JavaLearning, więc Ctrl+Shift+F (szukaj w całym projekcie) działa tak samo:
    //   nagłówek:  TEMAT: · W SKRÓCIE: · ANALOGIA: · JAK TO DZIAŁA: · SŁÓWKA: · ZOBACZ TEŻ:
    //   w treści:  PUŁAPKA: (typowy błąd i dlaczego) · DOBRA PRAKTYKA: (jak robić dobrze i dlaczego)
    //   na końcu:  ŚCIĄGA: · PYTANIA KONTROLNE: · ODPOWIEDZI: (w zwiniętym bloku — kliknij „+” na marginesie)
    //   ćwiczenia: ĆWICZENIE 1 (łatwe): … ĆWICZENIE 4 (trudniejsze): …
    //
    // DOBRA PRAKTYKA: szukaj po tagu z dwukropkiem, np. „PUŁAPKA:” — dlaczego? Bo słowo bez dwukropka występuje też
    //   w zwykłym tekście, a z dwukropkiem tylko w oznaczonych miejscach. Dostajesz listę wszystkich pułapek kursu.

    // =================================================================================================
    // 2. NAZWY — moduł, pakiet, lekcja, test
    // =================================================================================================
    //
    // Nazwy w kursie są przewidywalne. Znając jedną, wyliczysz pozostałe (klasa CourseNames robi to w kodzie):
    //   "s04-web-rest"  → pakiet "s04_web_rest"                 (CourseNames.sectionPackageOf)
    //   "s04-web-rest"  → numer działu 4                        (CourseNames.sectionNumberOf)
    //   "Start01HowToUse" → test "Start01HowToUseTest"          (CourseNames.testClassOf)
    //
    // PUŁAPKA: w nazwie pakietu Javy nie może być myślnika — „s04-web-rest” to poprawna nazwa MODUŁU (katalogu Mavena),
    //   ale jako pakiet byłaby błędem kompilacji (kompilator czytałby myślnik jak minus). Dlatego moduł i pakiet
    //   różnią się jednym znakiem.

    // =================================================================================================
    // 3. TEST ZAMIAST // WYNIK:
    // =================================================================================================
    //
    // W JavaLearning uruchamiałeś main i porównywałeś wydruk z komentarzem // WYNIK:. W Springu większość kodu
    // nie ma sensownego main (kontroler czeka na zapytania HTTP, serwis jest wołany przez kontroler), więc wynik
    // sprawdzamy TESTEM. Każda sekcja lekcji ma swoją metodę testową z opisem po polsku (@DisplayName).
    //
    // Jak czytać test (szczegóły w Start04TestingIntro):
    //   assertThat(CourseNames.sectionNumberOf("s04-web-rest")).isEqualTo(4);
    //   └── „upewnij się, że numer działu wyliczony z s04-web-rest jest równy 4”
    //
    // DOBRA PRAKTYKA: zanim uruchomisz test, przeczytaj asercję i PRZEWIDŹ, czy przejdzie. Dlaczego? Przewidywanie
    //   zmusza do zrozumienia kodu; samo patrzenie na zielony pasek uczy niewiele.

    // =================================================================================================
    // 4. METODA NAUKI — powtórki rozłożone w czasie
    // =================================================================================================
    //
    // Po lekcji zaplanuj powtórki: po 1, 3, 7, 14 i 30 dniach (CourseNames.reviewDates). Na powtórce najpierw
    // odpowiadasz na PYTANIA KONTROLNE z pamięci, dopiero potem zaglądasz do ŚCIĄGI.
    //
    // PUŁAPKA: „przeczytałem, rozumiem” to nie to samo co „pamiętam”. Dlaczego? Rozpoznawanie tekstu jest łatwe,
    //   przypominanie — trudne. Pamięć wzmacnia właśnie wysiłek przypominania, dlatego pytania są przed ściągą.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dział = moduł Mavena (sNN-temat), pakiet działu = sNN_temat, lekcja = podpakiet, test = ten sam pakiet w src/test/java.
     *   • Tagi z dwukropkiem: TEMAT:, W SKRÓCIE:, ANALOGIA:, JAK TO DZIAŁA:, SŁÓWKA:, ZOBACZ TEŻ:, PUŁAPKA:,
     *     DOBRA PRAKTYKA:, ŚCIĄGA:, PYTANIA KONTROLNE:, ODPOWIEDZI: — szukaj Ctrl+Shift+F.
     *   • Wynikiem lekcji jest test: zielony = kod robi to, co opisuje lekcja.
     *   • Przed uruchomieniem testu przewiduj wynik.
     *   • Powtórki: +1, +3, +7, +14, +30 dni; najpierw pytania z pamięci, potem ściąga.
     *
     * PYTANIA KONTROLNE:
     *   1. Jak nazywa się pakiet Javy działu, którego moduł to „s13-rest-client”? Dlaczego nazwy się różnią?
     *   2. Gdzie leży test lekcji Start01HowToUse (katalog i pakiet)?
     *   3. Co się stanie? CourseNames.sectionNumberOf("s16_cache_scheduling_async") — co zwróci?
     *   4. Co się stanie? CourseNames.reviewDates(LocalDate.of(2026, 1, 30)).get(1) — jaka data?
     *   5. ZNAJDŹ BŁĄD: ktoś utworzył w module s04-web-rest pakiet {@code package s04-web-rest.rest01_hello;} — co jest nie tak?
     *   6. Dlaczego w Springu wynik sprawdzamy testem, a nie komentarzem // WYNIK: pod main?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Start01HowToUseExercisesTest (uruchamiasz sam, ▶ w IntelliJ)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): nazwa pakietu działu → nazwa modułu (odwrotność CourseNames.sectionPackageOf).
     * Przykład: "s04_web_rest" → "s04-web-rest". Podpowiedź: String.replace(char, char).
     */
    static String exercise1(String sectionPackage) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nazwa klasy lekcji → nazwa jej pakietu. Przed każdą wielką literą (oprócz pierwszej) wstaw
     * podkreślnik, całość zamień na małe litery. Przykład: "Ioc02ConstructorInjection" → "ioc02_constructor_injection".
     * Podpowiedź: pętla po znakach + Character.isUpperCase + StringBuilder (t04_strings/Strings03StringBuilder).
     */
    static String exercise2(String lessonClass) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): czy dzień {@code day} jest dniem powtórki materiału nauczonego {@code learnedOn}?
     * Przykład: nauczone 1 marca → 2, 4, 8, 15 i 31 marca to dni powtórek; 1 i 5 marca — nie.
     * Podpowiedź: CourseNames.reviewDates(...).contains(...).
     */
    static boolean exercise3(LocalDate learnedOn, LocalDate day) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): ścieżka pliku testu lekcji względem korzenia repozytorium.
     * Dane: moduł "s04-web-rest" i klasa lekcji "Rest01Hello" →
     * "s04-web-rest/src/test/java/s04_web_rest/rest01_hello/Rest01HelloTest.java".
     * Podpowiedź: połącz CourseNames.sectionPackageOf, swoje ćwiczenie 2 i CourseNames.testClassOf.
     */
    static String exercise4(String moduleName, String lessonClass) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. „s13_rest_client”. Moduł to katalog Mavena — myślnik jest tam dozwolony. Pakiet to nazwa w Javie — myślnik
     *      byłby odczytany jak minus, więc zamieniamy go na podkreślnik.
     *   2. s00-start/src/test/java, pakiet s00_start.start01_how_to_use — ten sam pakiet co lekcja, dzięki czemu test
     *      widzi metody i klasy z dostępem pakietowym (bez public).
     *   3. 16 — metoda bierze znaki na pozycjach 1 i 2 („16”) i zamienia je na liczbę; reszta nazwy nie ma znaczenia.
     *   4. 2026-02-02 — element o indeksie 1 to druga powtórka (+3 dni); 30 stycznia + 3 dni przechodzi na luty.
     *      Sprawdzone testem w Start01HowToUseTest.
     *   5. Myślnik w nazwie pakietu to błąd kompilacji. Poprawnie: {@code package s04_web_rest.rest01_hello;}.
     *   6. Kod springowy zwykle nie ma sensownego main — kontrolery i serwisy wywołuje framework. Test uruchamia
     *      ten kod tak, jak zrobi to aplikacja, i SAM porównuje wynik z oczekiwanym — nie musisz porównywać wzrokiem.
     */
    // </editor-fold>
}
