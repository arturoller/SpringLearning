package s00_start.start07_glossary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Start07Glossary. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Zwykłe testy jednostkowe: Spring nie jest tu potrzebny, więc test jest szybki (milisekundy).
 */
@Tag("cwiczenie")
class Start07GlossaryExercisesTest {

    private static final List<Term> TERMS = List.of(
            new Term("Bean", "bean (ziarno)", "def", "s01_core_ioc/Ioc01Container"),
            new Term("Scope", "zasięg", "def", "s01_core_ioc/Ioc06Scopes"),
            new Term("Actuator", "Actuator (monitorowanie)", "def", "s15_actuator_logging"),
            new Term("Profile", "profil", "def", "s02_configuration/Cfg03Profiles"),
            new Term("Aspect", "aspekt", "def", "s18_aop"),
            new Term("Dependency injection", "wstrzykiwanie zależności", "def", "s01_core_ioc/Ioc02ConstructorInjection"));

    @Test
    @DisplayName("Ćwiczenie 1: wyszukiwanie po polskiej nazwie bez względu na wielkość liter")
    void exercise1() {
        assertThat(Start07Glossary.exercise1(TERMS, "BEAN")).extracting(Term::english).containsExactly("Bean");
        assertThat(Start07Glossary.exercise1(TERMS, "ASPE")).extracting(Term::english).containsExactly("Aspect");
        assertThat(Start07Glossary.exercise1(TERMS, "zzz")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: grupowanie po dziale, klucze i listy posortowane")
    void exercise2() {
        Map<String, List<String>> grouped = Start07Glossary.exercise2(TERMS);
        assertThat(grouped.keySet()).containsExactly("s01_core_ioc", "s02_configuration", "s15_actuator_logging", "s18_aop");
        assertThat(grouped.get("s01_core_ioc")).containsExactly("Bean", "Dependency injection", "Scope");
        assertThat(grouped.get("s18_aop")).containsExactly("Aspect");
    }

    @Test
    @DisplayName("Ćwiczenie 3: losowanie z ziarnem jest powtarzalne")
    void exercise3() {
        Term expected = TERMS.get(new Random(7).nextInt(TERMS.size()));
        assertThat(Start07Glossary.exercise3(TERMS, 7)).isEqualTo(expected);
        assertThat(Start07Glossary.exercise3(TERMS, 7)).isEqualTo(Start07Glossary.exercise3(TERMS, 7));
        Set<Term> distinct = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            distinct.add(Start07Glossary.exercise3(TERMS, seed));
        }
        assertThat(distinct.size()).as("różne ziarna dają różne hasła").isGreaterThan(1);
        assertThatThrownBy(() -> Start07Glossary.exercise3(List.of(), 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
