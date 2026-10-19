package s22_extras.ext06_native_image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext06NativeImage: cała aplikacja na zwykłej JVM (bez serwera WWW — webEnvironment = NONE).
 * ApplicationRunner uruchamia się także w teście, więc w logu są obie promocje jako JSON.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
class Ext06NativeImageAppTest {

    @Test
    @DisplayName("1. Aplikacja na JVM: promocje z pliku (refleksja) zastosowane do 349.90 zł i wypisane jako JSON (Jackson)")
    void runnerPrintsPromotions(CapturedOutput output) {
        assertThat(output.getAll())
                .contains("Promocja: {\"description\":\"10% taniej\",\"priceBefore\":349.90,\"priceAfter\":314.91}")
                .contains("Promocja: {\"description\":\"20 zł mniej\",\"priceBefore\":349.90,\"priceAfter\":329.90}");
    }
}
