package s12_security.sec01_basics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Sec01Basics: {@code SecurityContextHolder} to schowek na wątek (ThreadLocal). Zwykły test jednostkowy —
 * do tego nie trzeba Springa. Po każdym teście czyścimy schowek, żeby nie "przeciekał" do innych testów w tym samym wątku.
 */
class Sec01SecurityContextTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sekcja 2: kontekst ustawiony w wątku jest widoczny w tym wątku, a w innym wątku jest pusty")
    void contextIsPerThread() throws InterruptedException {
        Authentication anna = UsernamePasswordAuthenticationToken.authenticated(
                "anna", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(anna);
        SecurityContextHolder.setContext(context);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("anna");

        AtomicReference<Authentication> seenInOtherThread = new AtomicReference<>(anna);
        Thread other = Thread.ofVirtual().start(
                () -> seenInOtherThread.set(SecurityContextHolder.getContext().getAuthentication()));
        other.join();
        assertThat(seenInOtherThread.get()).as("inny wątek ma własny, pusty schowek").isNull();
    }

    @Test
    @DisplayName("Sekcja 2: clearContext() czyści schowek — tak robi SecurityContextHolderFilter po każdym żądaniu")
    void clearContext() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("anna", null, AuthorityUtils.NO_AUTHORITIES));
        SecurityContextHolder.clearContext();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
