package s19_deploy.dep02_docker;

import java.util.ArrayList;
import java.util.List;

/**
 * Pokazowa (demo) klasa lekcji Dep02Docker: proste odczytywanie faktów z tekstu pliku Dockerfile.
 * Używa jej test lekcji, żeby sprawdzić prawdziwy Dockerfile modułu. (Ćwiczenia proszą o inne funkcje.)
 */
public final class DockerfileFacts {

    private DockerfileFacts() {
    }

    /** Instrukcje pliku (FROM, COPY, RUN…) — bez komentarzy i pustych linii, z połączonymi liniami kończącymi się „\”. */
    public static List<String> instructions(String dockerfile) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String raw : dockerfile.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.endsWith("\\")) {
                current.append(line, 0, line.length() - 1).append(' ');
                continue;
            }
            current.append(line);
            result.add(current.toString().strip());
            current.setLength(0);
        }
        return result;
    }

    /** Liczba etapów budowy = liczba instrukcji FROM. */
    public static int stageCount(String dockerfile) {
        return (int) instructions(dockerfile).stream().filter(i -> i.startsWith("FROM ")).count();
    }

    /** Ostatnia instrukcja USER (użytkownik, z którego uprawnieniami działa kontener) albo "root", gdy jej nie ma. */
    public static String lastUser(String dockerfile) {
        String user = "root";   // bez USER kontener działa jako root — to wartość domyślna Dockera
        for (String instruction : instructions(dockerfile)) {
            if (instruction.startsWith("USER ")) {
                user = instruction.substring(5).strip();
            }
        }
        return user;
    }

    /** Czy ENTRYPOINT jest w formie „exec” (tablica JSON), a nie „shell” (zwykły tekst)? Brak ENTRYPOINT = false. */
    public static boolean hasExecFormEntrypoint(String dockerfile) {
        return instructions(dockerfile).stream()
                .filter(i -> i.startsWith("ENTRYPOINT "))
                .anyMatch(i -> i.substring(11).strip().startsWith("["));
    }
}
