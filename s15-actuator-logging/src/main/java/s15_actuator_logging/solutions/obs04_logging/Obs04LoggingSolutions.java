package s15_actuator_logging.solutions.obs04_logging;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Obs04Logging — zajrzyj dopiero po własnej próbie! */
public final class Obs04LoggingSolutions {

    private static final List<String> LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    private Obs04LoggingSolutions() {
    }

    static boolean solution1(String loggerLevel, String messageLevel) {
        int logger = LEVELS.indexOf(loggerLevel.toUpperCase());
        int message = LEVELS.indexOf(messageLevel.toUpperCase());
        if (logger < 0 || message < 0) {
            throw new IllegalArgumentException("Nieznany poziom: " + loggerLevel + " / " + messageLevel);
        }
        return message >= logger;
    }

    static String solution2(String cardNumber) {
        if (cardNumber == null) {
            return "brak";
        }
        long digits = cardNumber.chars().filter(Character::isDigit).count();
        long toMask = Math.max(digits - 4, 0) == 0 ? digits : digits - 4;   // 4 lub mniej cyfr → maskuj wszystkie
        StringBuilder result = new StringBuilder();
        long seen = 0;
        for (char c : cardNumber.toCharArray()) {
            if (Character.isDigit(c)) {
                result.append(seen < toMask ? '*' : c);
                seen++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    static String solution3(Map<String, String> configured, String loggerName) {
        String name = loggerName;
        while (true) {
            String level = configured.get(name);
            if (level != null) {
                return level;
            }
            int dot = name.lastIndexOf('.');
            if (dot < 0) {
                break;
            }
            name = name.substring(0, dot);   // przechodzimy do przodka: app.shop.pay.Charge → app.shop.pay
        }
        return configured.getOrDefault("ROOT", "INFO");
    }
}
