package s19_deploy.dep01_executable_jar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Dep01ExecutableJar — jednocześnie klasa główna (mainClass) JAR-a całego modułu
 * (patrz sekcja build w s19-deploy/pom.xml). Uruchom ją ▶ albo zbuduj JAR i odpal {@code java -jar}.
 */
@SpringBootApplication
public class Dep01Application {

    public static void main(String[] args) {
        SpringApplication.run(Dep01Application.class, args);
    }
}
