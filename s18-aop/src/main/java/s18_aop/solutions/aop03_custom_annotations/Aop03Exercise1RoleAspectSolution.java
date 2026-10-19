package s18_aop.solutions.aop03_custom_annotations;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import s18_aop.aop03_custom_annotations.CurrentUser;
import s18_aop.aop03_custom_annotations.PermissionDeniedException;
import s18_aop.aop03_custom_annotations.RequiresRole;

/**
 * Rozwiązanie wzorcowe ćwiczenia 1 lekcji Aop03CustomAnnotations. {@code @Before} wystarcza: wyjątek rzucony przez
 * poradę „przed” zatrzymuje wywołanie — metoda się nie wykona.
 * Uwaga: adnotacja RequiresRole jest w innym pakiecie, a mimo to wyrażenie ma krótką nazwę parametru — w formie
 * z parametrem typ adnotacji Spring bierze z TYPU PARAMETRU porady (zwykła Java, więc import działa).
 */
@Aspect
public class Aop03Exercise1RoleAspectSolution {

    private final CurrentUser currentUser;

    public Aop03Exercise1RoleAspectSolution(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(RequiresRole requiresRole) {
        if (!requiresRole.value().equals(currentUser.role())) {
            throw new PermissionDeniedException("Brak uprawnień: wymagana rola " + requiresRole.value()
                    + ", a użytkownik ma rolę " + currentUser.role());
        }
    }
}
