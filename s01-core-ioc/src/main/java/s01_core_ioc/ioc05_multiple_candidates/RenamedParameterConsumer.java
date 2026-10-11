package s01_core_ioc.ioc05_multiple_candidates;

/** Demo (bez adnotacji): to samo co ByParameterNameConsumer, ale parametr nazwano "sender" — nazwa nie pasuje do żadnego beana. */
public class RenamedParameterConsumer {

    private final NotificationSender sender;

    public RenamedParameterConsumer(NotificationSender sender) {
        this.sender = sender;
    }

    public NotificationSender sender() {
        return sender;
    }
}
