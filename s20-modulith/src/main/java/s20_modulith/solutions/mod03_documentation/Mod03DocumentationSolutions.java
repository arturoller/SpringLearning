package s20_modulith.solutions.mod03_documentation;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterface;
import org.springframework.modulith.docs.Documenter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Rozwiązania wzorcowe ćwiczeń z lekcji Mod03Documentation. Pakiet {@code solutions} leży poza pakietem głównym
 * {@code s20_modulith.mod03_documentation}, więc nie jest modułem tej aplikacji.
 */
public final class Mod03DocumentationSolutions {

    private Mod03DocumentationSolutions() {
    }

    /** ĆWICZENIE 1: sam diagram wszystkich modułów w podanym folderze. */
    public static void solution1(ApplicationModules modules, String outputFolder) {
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(outputFolder))
                .writeModulesAsPlantUml();
    }

    /** ĆWICZENIE 2: posortowane nazwy nazwanych interfejsów modułu. */
    public static List<String> solution2(ApplicationModules modules, String moduleName) {
        ApplicationModule module = modules.getModuleByName(moduleName)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma modułu: " + moduleName));
        return module.getNamedInterfaces().stream()
                .filter(NamedInterface::isNamed)
                .map(NamedInterface::getName)
                .sorted()
                .toList();
    }

    /** ĆWICZENIE 3: kanwy wszystkich modułów razem z wnętrzem. */
    public static void solution3(ApplicationModules modules, String outputFolder) {
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(outputFolder))
                .writeModuleCanvases(Documenter.CanvasOptions.defaults().revealInternals());
    }

    /** ĆWICZENIE 4: mapa „moduł → moduły, od których bezpośrednio zależy”. */
    public static Map<String, List<String>> solution4(ApplicationModules modules) {
        Map<String, List<String>> map = new TreeMap<>();
        modules.forEach(module -> map.put(module.getIdentifier().toString(),
                module.getDirectDependencies(modules).uniqueModules()
                        .map(dependency -> dependency.getIdentifier().toString())
                        .sorted()
                        .toList()));
        return map;
    }
}
