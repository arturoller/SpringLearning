package s18_aop.solutions.aop01_aspects;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Aop01Aspects — zajrzyj dopiero po własnej próbie! */
public final class Aop01AspectsSolutions {

    private Aop01AspectsSolutions() {
    }

    /**
     * {@code *} — dowolny typ zwracany; potem pełna nazwa klasy; {@code find*} — nazwa zaczynająca się od „find”;
     * {@code (String)} — DOKŁADNIE jeden parametr typu String (java.lang można pisać krótko).
     */
    public static String solution2Pointcut() {
        return "execution(* s18_aop.aop01_aspects.ProductCatalog.find*(String))";
    }
}
