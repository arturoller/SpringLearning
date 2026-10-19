package s12_security.solutions.sec06_security_testing;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec06SecurityTesting (WithMockAdmin): meta-adnotacja @WithMockUser. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithMockUser(username = "anna", roles = "ADMIN")
public @interface SolutionWithMockAdmin {
}
