package s02_configuration.cfg03_profiles;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bean istniejący tylko w profilu {@code default} — czyli gdy NIE włączono ŻADNEGO profilu. Gdy włączysz
 * jakikolwiek profil (nawet cfg03), profil „default” przestaje być aktywny i ten bean znika.
 */
@Component
@Profile("default")
public class DefaultProfileMarker {
}
