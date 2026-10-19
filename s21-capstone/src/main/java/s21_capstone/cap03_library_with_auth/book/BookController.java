package s21_capstone.cap03_library_with_auth.book;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap03_library_with_auth.book.BookDtos.BookResponse;
import s21_capstone.cap03_library_with_auth.book.BookDtos.NewBookRequest;

import java.net.URI;
import java.util.List;

/**
 * Katalog.
 * <pre>
 *   GET  /api/books       PUBLICZNY   200 lista (z polem available)
 *   GET  /api/books/{id}  PUBLICZNY   200 / 404
 *   POST /api/books       BIBLIOTEKARZ 201 + Location / 400 / 401 bez tokenu / 403 czytelnik / 409 sygnatura zajęta
 * </pre>
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> all() {
        return bookService.all();
    }

    @GetMapping("/{id}")
    public BookResponse one(@PathVariable long id) {
        return bookService.get(id);
    }

    @PostMapping
    public ResponseEntity<BookResponse> add(@Valid @RequestBody NewBookRequest request) {
        BookResponse created = bookService.add(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
