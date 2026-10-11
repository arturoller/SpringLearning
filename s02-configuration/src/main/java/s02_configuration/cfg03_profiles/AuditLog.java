package s02_configuration.cfg03_profiles;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dziennik audytu (audit log) — istnieje na produkcji LUB gdy włączono profil audytu.
 * {@code "cfg03-prod | cfg03-audit"} to wyrażenie profilowe: {@code |} = lub, {@code &} = i, {@code !} = nie.
 */
@Component
@Profile("cfg03-prod | cfg03-audit")
public class AuditLog {
}
