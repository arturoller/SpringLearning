package s01_core_ioc.ioc05_multiple_candidates;

/**
 * Demo (bez adnotacji): parametr nazywa się "smsSender" — tak samo jak bean. Gdy kandydatów jest kilku i żaden nie jest
 * {@code @Primary}, Spring dopasuje bean po NAZWIE PARAMETRU. Działa, bo kompilator zapisuje nazwy parametrów (-parameters).
 */
public class ByParameterNameConsumer {

    private final NotificationSender sender;

    public ByParameterNameConsumer(NotificationSender smsSender) {
        this.sender = smsSender;
    }

    public NotificationSender sender() {
        return sender;
    }
}
