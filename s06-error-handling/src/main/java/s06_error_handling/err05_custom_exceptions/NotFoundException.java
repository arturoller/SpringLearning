package s06_error_handling.err05_custom_exceptions;

/** Gałąź hierarchii: "nie ma takiego zasobu". W API zamienimy ją na 404. */
public abstract class NotFoundException extends ShopException {

    protected NotFoundException(String code, String message) {
        super(code, message);
    }
}
