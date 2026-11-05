package s12_security.solutions.sec02_authorization;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Sec02Authorization. */
public final class ExerciseRoleHierarchy {

    private ExerciseRoleHierarchy() {
    }

    public static RoleHierarchy create() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("EMPLOYEE", "AUDITOR")
                .role("EMPLOYEE").implies("CUSTOMER")
                .build();
    }
}
