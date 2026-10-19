package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

/**
 * UnvalidatedService = serwis z ograniczeniem na parametrze, ale BEZ {@code @Validated} na klasie. Bean nie jest
 * owijany w proxy walidacji, więc adnotacja {@code @NotBlank} jest tylko ozdobą (PUŁAPKA z lekcji).
 */
@Service
public class UnvalidatedService {

    public String shout(@NotBlank String text) {
        return text.toUpperCase() + "!";
    }
}
