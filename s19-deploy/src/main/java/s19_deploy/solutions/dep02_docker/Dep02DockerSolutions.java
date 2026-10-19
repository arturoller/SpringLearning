package s19_deploy.solutions.dep02_docker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Dep02Docker — zajrzyj dopiero po własnej próbie! */
public final class Dep02DockerSolutions {

    private Dep02DockerSolutions() {
    }

    static boolean solution1(String image) {
        int slash = image.lastIndexOf('/');
        int colon = image.lastIndexOf(':');
        if (colon <= slash) {
            return false;   // brak tagu (dwukropek, jeśli jest, należy do portu rejestru)
        }
        return !image.substring(colon + 1).equals("latest");
    }

    /** Instrukcja FROM po odrzuceniu opcji (--…): obraz i nazwa etapu (albo null). */
    private record FromLine(String image, String stageName) {
    }

    private static List<FromLine> fromLines(String dockerfile) {
        List<FromLine> result = new ArrayList<>();
        for (String raw : dockerfile.split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("#")) {
                continue;
            }
            String[] words = line.split("\\s+");
            if (words.length < 2 || !words[0].equalsIgnoreCase("FROM")) {
                continue;
            }
            List<String> args = new ArrayList<>();
            for (int i = 1; i < words.length; i++) {
                if (!words[i].startsWith("--")) {
                    args.add(words[i]);
                }
            }
            String stage = args.size() >= 3 && args.get(1).equalsIgnoreCase("AS") ? args.get(2) : null;
            result.add(new FromLine(args.get(0), stage));
        }
        return result;
    }

    static List<String> solution2(String dockerfile) {
        return fromLines(dockerfile).stream().map(FromLine::image).toList();
    }

    static List<String> solution3(String dockerfile) {
        List<String> problems = new ArrayList<>();
        Set<String> stages = new HashSet<>();
        for (FromLine from : fromLines(dockerfile)) {
            if (!stages.contains(from.image()) && !solution1(from.image())) {
                problems.add("NIEPRZYPIETY_OBRAZ:" + from.image());
            }
            if (from.stageName() != null) {
                stages.add(from.stageName());
            }
        }
        String user = null;
        boolean shellEntrypoint = false;
        for (String raw : dockerfile.split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("USER ")) {
                user = line.substring(5).strip();
            } else if (line.startsWith("ENTRYPOINT ") && !line.substring(11).strip().startsWith("[")) {
                shellEntrypoint = true;
            }
        }
        if (user == null || user.equals("root") || user.equals("0")) {
            problems.add("BRAK_USER_NIE_ROOT");
        }
        if (shellEntrypoint) {
            problems.add("ENTRYPOINT_W_FORMIE_SHELL");
        }
        return problems;
    }
}
