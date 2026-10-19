package s18_aop.aop03_custom_annotations;

/** Chwilowa awaria (temporary failure), np. magazyn nie odpowiada — warto spróbować jeszcze raz. Do ćwiczenia 3. */
public class TemporaryFailureException extends RuntimeException {

    public TemporaryFailureException(String message) {
        super(message);
    }
}
