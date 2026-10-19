package s21_capstone.cap03_library_with_auth.loan;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.LoanResponse;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.NewLoanRequest;

import java.net.URI;
import java.util.List;

/**
 * Wypożyczenia (wszystko wymaga tokenu JWT).
 * <pre>
 *   POST /api/loans                     BIBLIOTEKARZ  201 / 400 / 404 / 409 (wypożyczona, zaległości, limit)
 *   POST /api/loans/{id}/return         BIBLIOTEKARZ  200 z karą / 404 / 409 już zwrócona
 *   GET  /api/loans/{id}                bibliotekarz albo właściciel (@PostAuthorize)   200 / 403 / 404
 *   GET  /api/readers/{username}/loans  bibliotekarz albo ten czytelnik (@PreAuthorize) 200 / 403
 *   GET  /api/me/loans                  zalogowany — swoje wypożyczenia
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> lend(@Valid @RequestBody NewLoanRequest request) {
        LoanResponse created = loanService.lend(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/loans/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/loans/{id}/return")
    public LoanResponse returnBook(@PathVariable long id) {
        return loanService.returnBook(id);
    }

    @GetMapping("/loans/{id}")
    public LoanResponse one(@PathVariable long id) {
        return loanService.get(id);
    }

    @GetMapping("/readers/{username}/loans")
    public List<LoanResponse> ofReader(@PathVariable String username) {
        return loanService.loansOf(username);
    }

    /** {@code Authentication} wstrzykuje Spring MVC — przy tokenie JWT to JwtAuthenticationToken, a getName() = claim sub. */
    @GetMapping("/me/loans")
    public List<LoanResponse> mine(Authentication authentication) {
        return loanService.loansOf(authentication.getName());
    }
}
