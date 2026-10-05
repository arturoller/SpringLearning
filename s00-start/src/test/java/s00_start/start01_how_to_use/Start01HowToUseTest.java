package s00_start.start01_how_to_use;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Start01HowToUse. To zwykłe testy jednostkowe (unit tests): bez Springa, bo CourseNames to czysta Java.
 * Każda metoda testowa = jedna sekcja lekcji; @DisplayName (wyświetlana nazwa) mówi po polsku, co sekcja udowadnia.
 */
class Start01HowToUseTest {

    @Test
    @DisplayName("2. Nazwa modułu zamienia się w nazwę pakietu: myślniki → podkreślniki")
    void moduleToPackage() {
        assertThat(CourseNames.sectionPackageOf("s04-web-rest")).isEqualTo("s04_web_rest");
        assertThat(CourseNames.sectionPackageOf("s00-start")).isEqualTo("s00_start");
    }

    @Test
    @DisplayName("2. Numer działu to dwie cyfry po literze s — w module i w pakiecie")
    void sectionNumber() {
        assertThat(CourseNames.sectionNumberOf("s04-web-rest")).isEqualTo(4);
        assertThat(CourseNames.sectionNumberOf("s16_cache_scheduling_async")).isEqualTo(16);   // pytanie kontrolne 3
    }

    @Test
    @DisplayName("2. Test lekcji nazywa się jak lekcja + Test")
    void testClassName() {
        assertThat(CourseNames.testClassOf("Start01HowToUse")).isEqualTo("Start01HowToUseTest");
        // getSimpleName() = prosta nazwa klasy (bez pakietu) — ten test sprawdza konwencję na samym sobie
        assertThat(CourseNames.testClassOf(Start01HowToUse.class.getSimpleName()))
                .isEqualTo(Start01HowToUseTest.class.getSimpleName());
    }

    @Test
    @DisplayName("4. Powtórki wypadają po 1, 3, 7, 14 i 30 dniach")
    void reviewDates() {
        assertThat(CourseNames.reviewDates(LocalDate.of(2026, 3, 1))).containsExactly(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 8),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("4. Daty powtórek przechodzą przez koniec miesiąca (pytanie kontrolne 4)")
    void reviewDatesAcrossMonth() {
        assertThat(CourseNames.reviewDates(LocalDate.of(2026, 1, 30)).get(1)).isEqualTo(LocalDate.of(2026, 2, 2));
    }
}
