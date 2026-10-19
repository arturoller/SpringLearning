package s05_validation.val04_custom_validators;

import java.time.LocalDate;

/** HasDateRange = „ma zakres dat od–do”. Rekord, który to implementuje, może dostać adnotację {@link ValidDateRange}. */
public interface HasDateRange {

    LocalDate from();

    LocalDate to();
}
