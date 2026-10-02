import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LessonLint — static checks of SpringLearning lessons (structure-independent: works for any Maven module layout).
 * Run from anywhere inside the repository:  java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/LessonLint.java [glob ...]
 * Optional globs (relative to the repo root, forward slashes) limit the scope, e.g. "s04-web-rest/**".
 *
 * Checks: HTML entities, invisible characters, tag form (TAG must be followed by ':'), required tags in lesson classes,
 * collapsed answers fold, references sNN_pkg/Class (tools/lessons.txt) and tNN_pkg/Class (tools/javalearning-lessons.txt),
 * registry (lesson class listed in tools/lessons.txt, when the file exists), exercise tests tagged "cwiczenie" and
 * reference-solution tests tagged "wzorzec" in every lesson package that contains ĆWICZENIE.
 * Exit code: 0 = no problems, 1 = problems.
 */
public class LessonLint {

    static final Path ROOT = findRoot();
    static final Path TOOLS = ROOT.resolve("tools");
    static final List<String> problems = new ArrayList<>();

    /** Lesson class name convention: Topic + 2-digit number + Aspect, e.g. Ioc02ConstructorInjection. */
    static final Pattern LESSON_NAME = Pattern.compile("[A-Z][A-Za-z]*\\d\\d[A-Z][A-Za-z0-9]*\\.java");
    /**
     * Layout (CLAUDE.md): <module>/src/(main|test)/java/sNN_section/<lessonPackage>/... and
     * sNN_section/solutions/<lessonPackage>/... . Group 1 = section package, the rest = path inside it.
     */
    static final Pattern IN_SECTION = Pattern.compile(".*/src/(?:main|test)/java/(s\\d\\d_[a-z0-9_]+)/(.+)\\.java");

    /** lessonKey = "sNN_section/lessonPackage" (solutions/<lesson> mapped to the lesson), or "sNN_section/-" for files directly in the section. */
    static String lessonKey(String section, String inside) {
        String[] parts = inside.split("/");
        if (parts.length >= 3 && parts[0].equals("solutions")) return section + "/" + parts[1];
        if (parts.length >= 2 && !parts[0].equals("solutions")) return section + "/" + parts[0];
        return section + "/-";
    }

    public static void main(String[] args) throws IOException {
        List<java.nio.file.PathMatcher> scope = new ArrayList<>();
        for (String g : args) scope.add(ROOT.getFileSystem().getPathMatcher("glob:" + g.replace('\\', '/')));

        List<Path> files;
        try (Stream<Path> w = Files.walk(ROOT)) {
            files = w.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !rel(p).matches("(?:.*/)?(target|temp|tools|\\.git|\\.idea|\\.mvn)/.*") && !rel(p).startsWith("tools/"))
                    .filter(p -> scope.isEmpty() || scope.stream().anyMatch(m -> m.matches(Path.of(rel(p)))))
                    .sorted().collect(Collectors.toList());
        }

        List<String> req = new ArrayList<>(), lint = new ArrayList<>();
        String ex = (char) 0x0106 + "WICZENIE";   // fallback when tags.txt has no EX| line
        for (String t : Files.readAllLines(TOOLS.resolve("tags.txt"), StandardCharsets.UTF_8)) {
            if (t.startsWith("REQ|")) req.add(t.substring(4));
            else if (t.startsWith("EX|")) ex = t.substring(3);
            else if (t.startsWith("LINT|")) lint.add(t.substring(5));
        }
        Registry spring = Registry.load(TOOLS.resolve("lessons.txt"));
        Registry java = Registry.load(TOOLS.resolve("javalearning-lessons.txt"));

        Pattern entity = Pattern.compile("&(lt|gt|amp);");
        String invisibleChars = "" + (char) 0x00A0 + (char) 0x202F + (char) 0x2007 + (char) 0x200B + (char) 0xFEFF;
        Pattern springRef = Pattern.compile("\\b(s\\d\\d_[a-z0-9_]+)(?:/([A-Z][A-Za-z0-9]+))?");
        Pattern javaRef = Pattern.compile("\\b(t\\d\\d_[a-z_]+)(?:/([A-Z][A-Za-z0-9]+))?");

        // package → does it contain ĆWICZENIE / exercise tests / solution tests
        TreeMap<String, boolean[]> exercisePackages = new TreeMap<>();

        for (Path p : files) {
            String rel = rel(p);
            String text = Files.readString(p, StandardCharsets.UTF_8);
            if (entity.matcher(text).find()) problems.add("ENTITY    " + rel + " : HTML entity (use {@code ...})");
            for (char c : invisibleChars.toCharArray()) {
                if (text.indexOf(c) >= 0) {
                    problems.add("INVISIBLE " + rel + " : invisible character U+" + String.format("%04X", (int) c) + " (use an escape)");
                }
            }
            checkRefs(rel, text, springRef, spring, "lessons.txt");
            checkRefs(rel, text, javaRef, java, "javalearning-lessons.txt");
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                for (String t : lint) {
                    String q = Pattern.quote(t);
                    if (lines[i].matches("^\\s*(//|\\*)\\s*" + q + "(?!:)(\\s.*|$|[^A-Za-z].*)")
                            && !lines[i].matches("^\\s*(//|\\*)\\s*" + q + ":.*")) {
                        problems.add("TAGFORM   " + rel + ":" + (i + 1) + " : '" + lines[i].strip() + "'");
                    }
                }
            }

            Matcher sec = IN_SECTION.matcher(rel);
            if (!sec.matches()) continue;
            String section = sec.group(1);
            String lesson = lessonKey(section, sec.group(2));
            boolean isTest = rel.contains("/src/test/");
            boolean[] flags = exercisePackages.computeIfAbsent(lesson, k -> new boolean[3]);
            if (!isTest && text.contains(ex + " ")) flags[0] = true;
            if (isTest && text.contains("@Tag(\"cwiczenie\")")) flags[1] = true;
            if (isTest && text.contains("@Tag(\"wzorzec\")")) flags[2] = true;

            String fileName = p.getFileName().toString();
            if (!isTest && LESSON_NAME.matcher(fileName).matches() && text.contains("TEMAT:")) {
                for (String t : req) if (!text.contains(t)) problems.add("MISSING   " + rel + " : tag '" + t + "'");
                if (!text.contains("defaultstate=\"collapsed\"")) problems.add("MISSING   " + rel + " : collapsed ODPOWIEDZI fold");
                String key = section + "/" + fileName.replace(".java", "");
                if (spring.loaded && !spring.classes.contains(key)) problems.add("REGISTRY  " + rel + " : " + key + " not in tools/lessons.txt");
            }
        }
        exercisePackages.forEach((pkg, f) -> {
            if (f[0] && !f[1]) problems.add("EXERCISE  " + pkg + " : has exercises but no test tagged @Tag(\"cwiczenie\")");
            if (f[0] && !f[2]) problems.add("EXERCISE  " + pkg + " : has exercises but no solution test tagged @Tag(\"wzorzec\")");
        });

        System.out.println("FILES: " + files.size() + (spring.loaded ? "" : "  (tools/lessons.txt missing — registry checks skipped)"));
        System.out.println("PROBLEMS: " + problems.size());
        new TreeSet<>(problems).forEach(pr -> System.out.println("  " + pr));
        System.exit(problems.isEmpty() ? 0 : 1);
    }

    static void checkRefs(String rel, String text, Pattern ref, Registry reg, String name) {
        if (!reg.loaded) return;
        Matcher m = ref.matcher(text);
        while (m.find()) {
            if (!reg.packages.contains(m.group(1))) {
                problems.add("REF       " + rel + " : unknown package '" + m.group(1) + "' (" + name + ")");
            } else if (m.group(2) != null && !reg.classes.contains(m.group(1) + "/" + m.group(2))) {
                problems.add("REF       " + rel + " : unknown lesson '" + m.group(1) + "/" + m.group(2) + "' (" + name + ")");
            }
        }
    }

    record Registry(boolean loaded, Set<String> classes, Set<String> packages) {
        static Registry load(Path file) throws IOException {
            Set<String> classes = new HashSet<>(), packages = new HashSet<>();
            if (!Files.exists(file)) return new Registry(false, classes, packages);
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String entry = line.strip();
                packages.add(entry.split("/")[0]);
                if (!entry.endsWith("/*")) classes.add(entry);          // "sNN_section/*" registers only the section
            }
            return new Registry(true, classes, packages);
        }
    }

    static Path findRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !(Files.exists(p.resolve("CLAUDE.md")) && Files.isDirectory(p.resolve("tools")))) p = p.getParent();
        if (p == null) throw new IllegalStateException("Run LessonLint from inside the SpringLearning repository");
        return p;
    }

    static String rel(Path p) {
        return ROOT.relativize(p).toString().replace('\\', '/');
    }
}
