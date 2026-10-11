package s04_web_rest.solutions.rest01_controllers;

import java.util.Map;
import java.util.Set;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Rest01Controllers — zajrzyj dopiero po własnej próbie! */
public final class Rest01ControllersSolutions {

    private Rest01ControllersSolutions() {
    }

    static int solution3(Map<String, Set<String>> routes, String method, String path) {
        Set<String> allowed = routes.get(path);
        if (allowed == null) {
            return 404;                                   // nikt nie obsługuje tej ścieżki
        }
        String m = method.toUpperCase();
        if (allowed.contains(m) || m.equals("OPTIONS") || (m.equals("HEAD") && allowed.contains("GET"))) {
            return 200;
        }
        return 405;                                       // ścieżka znana, metoda HTTP nie
    }
}
