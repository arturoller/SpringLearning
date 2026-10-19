package s21_capstone.cap03_library_with_auth.book;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap03_library_with_auth.book.BookDtos.BookResponse;
import s21_capstone.cap03_library_with_auth.book.BookDtos.NewBookRequest;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.common.NotFoundException;

import java.util.List;
import java.util.Map;

/** Katalog: przeglądanie (publiczne) i dodawanie egzemplarzy (bibliotekarz). */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;

    public BookService(BookRepository books) {
        this.books = books;
    }

    public List<BookResponse> all() {
        return books.findAllByOrderByCallNumber().stream().map(BookResponse::from).toList();
    }

    public BookResponse get(long id) {
        return books.findById(id).map(BookResponse::from).orElseThrow(() -> new NotFoundException("książka", id));
    }

    /**
     * {@code @PreAuthorize} na serwisie MIMO reguły URL w łańcuchu filtrów — obrona w głąb (defense in depth): gdy ktoś
     * kiedyś wywoła tę metodę z innego miejsca (zadanie w tle, inny kontroler), reguła nadal obowiązuje.
     */
    @Transactional
    @PreAuthorize("hasRole('BIBLIOTEKARZ')")
    public BookResponse add(NewBookRequest request) {
        if (books.existsByCallNumber(request.callNumber())) {
            throw callNumberTaken(request.callNumber());
        }
        try {
            return BookResponse.from(books.saveAndFlush(
                    new Book(request.callNumber(), request.title().strip(), request.author().strip())));
        } catch (DataIntegrityViolationException raceLost) {
            throw callNumberTaken(request.callNumber());
        }
    }

    private static ConflictException callNumberTaken(String callNumber) {
        return new ConflictException("CALL_NUMBER_TAKEN", "Sygnatura " + callNumber + " jest już zajęta",
                Map.of("callNumber", callNumber));
    }
}
