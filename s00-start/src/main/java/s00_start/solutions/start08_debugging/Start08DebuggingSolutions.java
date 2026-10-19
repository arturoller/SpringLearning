package s00_start.solutions.start08_debugging;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Start08Debugging — zajrzyj dopiero po własnej próbie! */
public final class Start08DebuggingSolutions {

    private Start08DebuggingSolutions() {
    }

    /** Idziemy w dół łańcucha getCause(), aż do wyjątku bez przyczyny (z zabezpieczeniem przed cyklem). */
    static Throwable solution1(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
