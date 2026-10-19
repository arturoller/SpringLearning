package s19_deploy.solutions.dep01_executable_jar;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Dep01ExecutableJar — zajrzyj dopiero po własnej próbie! */
public final class Dep01ExecutableJarSolutions {

    private Dep01ExecutableJarSolutions() {
    }

    static String solution1(String jarName, Map<String, String> jvmProperties, Map<String, String> appArguments) {
        StringBuilder command = new StringBuilder("java");
        new TreeMap<>(jvmProperties).forEach((key, value) -> command.append(" -D").append(key).append('=').append(value));
        command.append(" -jar ").append(jarName);
        new TreeMap<>(appArguments).forEach((key, value) -> command.append(" --").append(key).append('=').append(value));
        return command.toString();
    }

    static long solution2(List<String> jarEntries) {
        return jarEntries.stream()
                .filter(entry -> entry.startsWith("BOOT-INF/lib/") && entry.endsWith(".jar"))
                .count();
    }

    static String solution3(String entryPath) {
        if (entryPath.startsWith("org/springframework/boot/loader/")) {
            return "spring-boot-loader";
        }
        if (entryPath.startsWith("BOOT-INF/lib/")) {
            return entryPath.endsWith("SNAPSHOT.jar") ? "snapshot-dependencies" : "dependencies";
        }
        return "application";
    }
}
