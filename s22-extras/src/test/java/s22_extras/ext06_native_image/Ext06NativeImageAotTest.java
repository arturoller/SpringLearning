package s22_extras.ext06_native_image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.javapoet.ClassName;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext06NativeImage, sekcja 2: przetwarzanie AOT „na żywo” w teście — to samo, co robi
 * {@code spring-boot:process-aot}, ale dla małej konfiguracji i z plikami w pamięci. Bez GraalVM.
 */
class Ext06NativeImageAotTest {

    /** Mała konfiguracja: jeden bean + te same adnotacje z podpowiedziami co w Ext06Application. */
    @Configuration(proxyBeanMethods = false)
    @ImportRuntimeHints(Ext06RuntimeHints.class)
    @RegisterReflectionForBinding(PromotionDto.class)
    static class AotDemoConfig {
        @Bean
        PromotionLoader promotionLoader() {
            return new PromotionLoader();
        }
    }

    static final String BEAN_DEFINITIONS = "s22_extras/ext06_native_image/Ext06NativeImageAotTest__BeanDefinitions.java";

    record AotResult(ClassName initializer, InMemoryGeneratedFiles files, RuntimeHints hints) {
    }

    static AotResult processAheadOfTime() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(AotDemoConfig.class);   // bez refresh() — AOT sam „odświeża na niby” (refreshForAotProcessing)
        InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
        DefaultGenerationContext generationContext = new DefaultGenerationContext(
                new ClassNameGenerator(ClassName.get(AotDemoConfig.class)), generatedFiles);

        ClassName initializer = new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
        generationContext.writeGeneratedContent();
        return new AotResult(initializer, generatedFiles, generationContext.getRuntimeHints());
    }

    @Test
    @DisplayName("2. AOT generuje KOD JAVY: inicjalizator kontekstu i definicje beanów wołające metody @Bean wprost")
    void generatesJavaSource() throws IOException {
        AotResult result = processAheadOfTime();

        assertThat(result.initializer()).hasToString(
                "s22_extras.ext06_native_image.Ext06NativeImageAotTest_AotDemoConfig__ApplicationContextInitializer");
        assertThat(result.files().getGeneratedFiles(GeneratedFiles.Kind.SOURCE)).containsKeys(
                "s22_extras/ext06_native_image/Ext06NativeImageAotTest_AotDemoConfig__ApplicationContextInitializer.java",
                BEAN_DEFINITIONS);
        assertThat(result.files().getGeneratedFileContent(GeneratedFiles.Kind.SOURCE, BEAN_DEFINITIONS))
                .contains("public static BeanDefinition getPromotionLoaderBeanDefinition()")
                .contains(".promotionLoader())")          // zwykłe wywołanie metody @Bean — bez refleksji
                .contains("@Generated");
    }

    @Test
    @DisplayName("2/3/4. AOT zbiera podpowiedzi z @ImportRuntimeHints i @RegisterReflectionForBinding")
    void collectsRuntimeHints() {
        RuntimeHints hints = processAheadOfTime().hints();

        assertThat(RuntimeHintsPredicates.resource().forResource(PromotionLoader.PROMOTIONS_FILE)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(FixedDiscountRule.class)
                .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PromotionDto.class, "description")).accepts(hints);
    }
}
