package s04_web_rest.rest02_request_data;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 6 lekcji Rest02RequestData: flaga kompilatora -parameters. Test kompiluje w locie (javax.tools.JavaCompiler —
 * kompilator Javy dostępny z kodu) ten sam mały kontroler DWA razy: bez flagi i z flagą, a potem wysyła do każdej
 * wersji to samo zapytanie przez MockMvc w trybie standalone (sam kontroler + infrastruktura MVC, bez Boota).
 */
class Rest02RequestDataParametersTest {

    private static final String SOURCE = """
            package demo;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class HelloController {

                @GetMapping("/api/hello")
                public String hello(@RequestParam String name) {   // nazwa "name" NIE jest podana w adnotacji
                    return "Hello, " + name;
                }
            }
            """;

    private URLClassLoader loader;   // ładuje skompilowaną w locie klasę; zamykamy po teście

    @AfterEach   // after each = po każdym teście
    void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    @DisplayName("6. Bez -parameters Spring nie zna nazwy parametru → IllegalArgumentException przy zapytaniu (pytanie 7)")
    void withoutParametersFlag(@TempDir Path dir) throws Exception {
        MockMvc mockMvc = compileAndSetUp(dir, List.of());   // kompilacja BEZ -parameters

        assertThatThrownBy(() -> mockMvc.perform(get("/api/hello").param("name", "Jan")))
                .isInstanceOf(ServletException.class)
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name for argument of type [java.lang.String] not specified, and parameter name information "
                        + "not available via reflection. Ensure that the compiler uses the '-parameters' flag.");
    }

    @Test
    @DisplayName("6. Z -parameters (tak kompiluje spring-boot-starter-parent) ten sam kod działa: 200 i „Hello, Jan”")
    void withParametersFlag(@TempDir Path dir) throws Exception {
        MockMvc mockMvc = compileAndSetUp(dir, List.of("-parameters"));

        mockMvc.perform(get("/api/hello").param("name", "Jan"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Jan"));
    }

    /** Kompiluje SOURCE do katalogu dir z podanymi opcjami, ładuje klasę i buduje MockMvc tylko dla niej. */
    private MockMvc compileAndSetUp(Path dir, List<String> extraOptions) throws Exception {
        Path sourceFile = dir.resolve("src/demo/HelloController.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, SOURCE, StandardCharsets.UTF_8);
        Path classes = Files.createDirectories(dir.resolve("classes"));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();   // null na samym JRE — testy biegną na JDK
        assertThat(compiler).as("Do tego testu potrzebny jest JDK (z kompilatorem javac)").isNotNull();

        List<String> args = new ArrayList<>(List.of("-proc:none", "-encoding", "UTF-8",
                "-classpath", springJars(), "-d", classes.toString()));
        args.addAll(extraOptions);
        args.add(sourceFile.toString());
        int exitCode = compiler.run(null, null, null, args.toArray(String[]::new));
        assertThat(exitCode).as("kompilacja HelloController").isZero();

        loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader());
        Object controller = loader.loadClass("demo.HelloController").getDeclaredConstructor().newInstance();
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** Ścieżka klas dla kompilatora: jary, z których pochodzą adnotacje użyte w kontrolerze (i ich metaadnotacje). */
    private static String springJars() {
        return Stream.of(RestController.class, Controller.class, AliasFor.class)
                .map(Rest02RequestDataParametersTest::jarOf)
                .distinct()
                .collect(Collectors.joining(File.pathSeparator));
    }

    private static String jarOf(Class<?> type) {
        try {
            return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
