package s06_error_handling.solutions.err02_exception_handler;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err02ExceptionHandler — zajrzyj dopiero po własnej próbie! */
public final class Err02ExceptionHandlerSolutions {

    private Err02ExceptionHandlerSolutions() {
    }

    static boolean solution1(Class<? extends Throwable> handled, Class<? extends Throwable> thrown) {
        return handled.isAssignableFrom(thrown);
    }

    static int solution2(Class<? extends Throwable> handled, Class<? extends Throwable> thrown) {
        int steps = 0;
        for (Class<?> c = thrown; c != null; c = c.getSuperclass()) {
            if (c == handled) {
                return steps;
            }
            steps++;
        }
        return -1;
    }

    static Class<? extends Throwable> solution3(List<Class<? extends Throwable>> handled, Class<? extends Throwable> thrown) {
        Class<? extends Throwable> best = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean tie = false;
        for (Class<? extends Throwable> candidate : handled) {
            int d = solution2(candidate, thrown);
            if (d < 0) {
                continue;
            }
            if (d < bestDistance) {
                best = candidate;
                bestDistance = d;
                tie = false;
            } else if (d == bestDistance) {
                tie = true;
            }
        }
        if (tie) {
            throw new IllegalStateException("Ambiguous handler for " + thrown.getName());
        }
        return best;
    }
}
