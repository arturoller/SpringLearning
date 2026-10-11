package s02_configuration.solutions.cfg03_profiles;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Rozwiązania wzorcowe ćwiczeń lekcji Cfg03Profiles — zajrzyj dopiero po własnej próbie! */
public final class Cfg03ProfilesSolutions {

    private Cfg03ProfilesSolutions() {
    }

    /** Interfejs jak w ćwiczeniu 1. */
    public interface MailSender {
        String send(String to);
    }

    /** Rozwiązanie 1a: negacja — wszędzie poza produkcją. */
    @Profile("!cfg03-prod")
    public static class ConsoleMailSender implements MailSender {
        @Override
        public String send(String to) {
            return "KONSOLA: " + to;
        }
    }

    /** Rozwiązanie 1b: tylko produkcja. */
    @Profile("cfg03-prod")
    public static class SmtpMailSender implements MailSender {
        @Override
        public String send(String to) {
            return "SMTP: " + to;
        }
    }

    /** Rozwiązanie 2: najpierw sprawdzamy najważniejszy tryb (produkcja), potem test. */
    public static String solution2(Environment environment) {
        if (environment.matchesProfiles("cfg03-prod")) {
            return "PRODUKCJA";
        }
        if (environment.matchesProfiles("cfg03-test")) {
            return "TEST";
        }
        return "ROZWÓJ";
    }

    /** Rozwiązanie 3: definicja grupy + włączenie profilu bazowego i grupy. */
    public static String[] solution3() {
        return new String[]{
                "spring.profiles.group.cfg03-full=cfg03-dev,cfg03-audit",
                "spring.profiles.active=cfg03,cfg03-full"
        };
    }
}
