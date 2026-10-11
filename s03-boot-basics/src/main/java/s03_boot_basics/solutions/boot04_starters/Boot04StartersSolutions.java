package s03_boot_basics.solutions.boot04_starters;

import s03_boot_basics.boot04_starters.ClasspathInspector;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot04Starters — zajrzyj dopiero po własnej próbie! */
public final class Boot04StartersSolutions {

    private Boot04StartersSolutions() {
    }

    static List<String> solution1(List<String> classNames) {
        return classNames.stream().filter(ClasspathInspector::isPresent).sorted().toList();
    }

    static String solution2(Class<?> type) {
        try {
            return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> solution3(String dependencyTree, String scope) {
        return dependencyTree.lines()
                .map(line -> line.replaceFirst("^[^a-z]+", ""))
                .map(line -> line.split(":"))
                .filter(parts -> parts.length == 5 && parts[4].equals(scope))
                .map(parts -> parts[1] + ":" + parts[3])
                .sorted()
                .toList();
    }
}
