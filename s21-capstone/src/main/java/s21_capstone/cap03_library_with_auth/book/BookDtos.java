package s21_capstone.cap03_library_with_auth.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO katalogu. */
public final class BookDtos {

    private BookDtos() {
    }

    /** POST /api/books (tylko BIBLIOTEKARZ). */
    public record NewBookRequest(
            @NotBlank(message = "sygnatura jest wymagana")
            @Pattern(regexp = "[A-Z]{3}-\\d{3}", message = "sygnatura ma format ABC-123")
            String callNumber,
            @NotBlank(message = "tytuł jest wymagany")
            @Size(max = 200, message = "tytuł: najwyżej 200 znaków")
            String title,
            @NotBlank(message = "autor jest wymagany")
            @Size(max = 100, message = "autor: najwyżej 100 znaków")
            String author) {
    }

    /** Książka w katalogu — publiczna: bez informacji, KTO ją wypożyczył (to dane osobowe). */
    public record BookResponse(Long id, String callNumber, String title, String author, boolean available) {

        public static BookResponse from(Book book) {
            return new BookResponse(book.getId(), book.getCallNumber(), book.getTitle(), book.getAuthor(),
                    !book.isOnLoan());
        }
    }
}
