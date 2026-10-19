package s00_start.solutions.start07_glossary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s00_start.start07_glossary.Term;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Te same sprawdzenia co w Start07GlossaryExercisesTest, ale na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start07GlossarySolutionsTest {

    private static final List<Term> TERMS = List.of(
            new Term("Bean", "bean (ziarno)", "def", "s01_core_ioc/Ioc01Container"),
            new Term("Scope", "zasięg", "def", "s01_core_ioc/Ioc06Scopes"),
            new Term("Actuator", "Actuator (monitorowanie)", "def", "s15_actuator_logging"),
            new Term("Profile", "profil", "def", "s02_configuration/Cfg03Profiles"),
            new Term("Aspect", "aspekt", "def", "s18_aop"),
            new Term("Dependency injection", "wstrzykiwanie zależności", "def", "s01_core_ioc/Ioc02ConstructorInjection"));

    @Test
    @DisplayName("Wzorzec 1: wyszukiwanie po polskiej nazwie bez względu na wielkość liter")
    void solution1() {
        assertThat(Start07GlossarySolutions.solution1(TERMS, "BEAN")).extracting(Term::english).containsExactly("Bean");
        assertThat(Start07GlossarySolutions.solution1(TERMS, "ASPE")).extracting(Term::english)
                .containsExactly("Aspect");
        assertThat(Start07GlossarySolutions.solution1(TERMS, "zzz")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: grupowanie po dziale, klucze i listy posortowane")
    void solution2() {
        Map<String, List<String>> grouped = Start07GlossarySolutions.solution2(TERMS);
        assertThat(grouped.keySet()).containsExactly("s01_core_ioc", "s02_configuration", "s15_actuator_logging", "s18_aop");
        assertThat(grouped.get("s01_core_ioc")).containsExactly("Bean", "Dependency injection", "Scope");
        assertThat(grouped.get("s18_aop")).containsExactly("Aspect");
    }

    @Test
    @DisplayName("Wzorzec 3: losowanie z ziarnem jest powtarzalne")
    void solution3() {
        Term expected = TERMS.get(new Random(7).nextInt(TERMS.size()));
        assertThat(Start07GlossarySolutions.solution3(TERMS, 7)).isEqualTo(expected);
        assertThat(Start07GlossarySolutions.solution3(TERMS, 7)).isEqualTo(Start07GlossarySolutions.solution3(TERMS, 7));
        Set<Term> distinct = new java.util.HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            distinct.add(Start07GlossarySolutions.solution3(TERMS, seed));
        }
        assertThat(distinct.size()).as("różne ziarna dają różne hasła").isGreaterThan(1);
        assertThatThrownBy(() -> Start07GlossarySolutions.solution3(List.of(), 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
