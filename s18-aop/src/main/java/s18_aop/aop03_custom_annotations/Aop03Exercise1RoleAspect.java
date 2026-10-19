package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * ĆWICZENIE 1 (łatwe) lekcji Aop03CustomAnnotations — opis w klasie lekcji. Bez {@code @Component}: test zakłada
 * aspekt ręcznie (AspectJProxyFactory), więc nierozwiązane ćwiczenie nie wpływa na testy lekcji.
 */
@Aspect
public class Aop03Exercise1RoleAspect {

    private final CurrentUser currentUser;

    public Aop03Exercise1RoleAspect(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(RequiresRole requiresRole) {
        // TODO: jeśli currentUser.role() różni się od requiresRole.value(), rzuć PermissionDeniedException
        //       z komunikatem "Brak uprawnień: wymagana rola KIEROWNIK, a użytkownik ma rolę KLIENT"
        //       (role wstaw z adnotacji i z currentUser); gdy rola się zgadza — nic nie rób
        throw new UnsupportedOperationException("TODO");
    }
}
