package s00_start.solutions.start03_first_application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rozwiązania wzorcowe ćwiczeń lekcji Start03FirstApplication — zajrzyj dopiero po własnej próbie! */
public final class Start03FirstApplicationSolutions {

    private static final Pattern PORT = Pattern.compile("port (\\d+)");

    private Start03FirstApplicationSolutions() {
    }

    static String solution1(String name) {
        if (name == null || name.isBlank()) {
            return "Cześć, nieznajomy!";
        }
        return "Cześć, " + name.strip() + "!";
    }

    static int solution2(String logLine) {
        Matcher matcher = PORT.matcher(logLine);
        if (!matcher.find()) {
            throw new IllegalArgumentException("W linii nie ma numeru portu: " + logLine);
        }
        return Integer.parseInt(matcher.group(1));
    }

    static String solution3(String module, String mainClass, boolean windows) {
        if (!mainClass.endsWith("Application")) {
            throw new IllegalArgumentException("To raczej nie jest klasa startowa: " + mainClass);
        }
        String wrapper = windows ? "mvnw.cmd" : "./mvnw";
        return wrapper + " -pl " + module + " spring-boot:run -Dspring-boot.run.main-class=" + mainClass;
    }
}
