package s00_start.solutions.start07_glossary;

import s00_start.start07_glossary.Term;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Start07Glossary — zajrzyj dopiero po własnej próbie! */
public final class Start07GlossarySolutions {

    private Start07GlossarySolutions() {
    }

    static List<Term> solution1(List<Term> terms, String fragment) {
        String needle = fragment.toLowerCase(Locale.ROOT);
        return terms.stream()
                .filter(term -> term.polish().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    static Map<String, List<String>> solution2(List<Term> terms) {
        Map<String, List<String>> bySection = new TreeMap<>();
        for (Term term : terms) {
            int slash = term.lesson().indexOf('/');
            String section = slash < 0 ? term.lesson() : term.lesson().substring(0, slash);
            bySection.computeIfAbsent(section, key -> new ArrayList<>()).add(term.english());
        }
        bySection.values().forEach(Collections::sort);
        return bySection;
    }

    static Term solution3(List<Term> terms, long seed) {
        if (terms.isEmpty()) {
            throw new IllegalArgumentException("Pusta lista haseł");
        }
        return terms.get(new Random(seed).nextInt(terms.size()));
    }
}
