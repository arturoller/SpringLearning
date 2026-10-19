package s21_capstone.cap03_library_with_auth.loan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO wypożyczeń. */
public final class LoanDtos {

    private LoanDtos() {
    }

    /** POST /api/loans (BIBLIOTEKARZ): który egzemplarz i któremu czytelnikowi (login). */
    public record NewLoanRequest(
            @NotNull(message = "książka jest wymagana")
            Long bookId,
            @NotBlank(message = "czytelnik jest wymagany")
            String reader) {
    }

    /**
     * Wypożyczenie w odpowiedzi. {@code daysOverdue} i {@code fine} liczone na DZIŚ (z zegara) dla aktywnych,
     * a dla zwróconych — kara zapisana przy zwrocie.
     */
    public record LoanResponse(Long id, String callNumber, String title, String reader, LocalDate loanedOn,
                               LocalDate dueOn, LocalDate returnedOn, boolean extended, long daysOverdue,
                               BigDecimal fine) {

        public static LoanResponse from(Loan loan, LocalDate today, FineCalculator fines) {
            LocalDate end = loan.isActive() ? today : loan.getReturnedOn();
            BigDecimal fine = loan.isActive() ? fines.fineFor(loan.getDueOn(), today) : loan.getFine();
            return new LoanResponse(loan.getId(), loan.getBook().getCallNumber(), loan.getBook().getTitle(),
                    loan.getReader().getUsername(), loan.getLoanedOn(), loan.getDueOn(), loan.getReturnedOn(),
                    loan.isExtended(), fines.daysOverdue(loan.getDueOn(), end), fine);
        }
    }
}
