# AGENT KIT (SpringLearning) — everything a sub-agent needs to write lessons (read ONLY this file + your outline)

The main session gives you: this kit + ONE outline section from `temp/ASSIGNMENTS.md` (or its text in the prompt).
Do not explore the repository beyond what your outline names. Never run environment-check commands (`java -version`, `ls`...).
Versions and starter names: `CLAUDE.md` sections 2–3 (Spring Boot 4.1.x, Java 21 baseline). Read them if your outline does not
repeat them.

## 1. Rules
1. Create/edit ONLY the files of your assignment (your module / lesson packages) and add your lesson names to
   `tools/lessons.txt`. No git. No sub-agents. Never edit other lessons, README.md, CLAUDE.md or other modules. Edit the parent
   `pom.xml` ONLY to add your module to `<modules>` if the outline says you create the module.
2. Audience: a Polish learner with weak English. ALL comments, `@DisplayName`s and printed text in Polish with diacritics;
   identifiers in English. EVERY English identifier/annotation/API gets a Polish translation at its first use in the file:
   `@Transactional // transactional = transakcyjny`, `bean (ziarno — obiekt zarządzany przez Springa)`.
   No calques: immutable = „niezmienny” (NOT „niemutowalny”), mutable = „zmienny”, shadowing/hiding = „przesłanianie/ukrywanie”,
   side effect = „efekt uboczny”, dependency injection = „wstrzykiwanie zależności”, endpoint = „punkt końcowy (endpoint)”.
3. Tags verbatim WITH the colon directly after the word, at the start of a comment line:
   header `TEMAT:` `W SKRÓCIE:` `ANALOGIA:` `JAK TO DZIAŁA:` `SŁÓWKA:` `ZOBACZ TEŻ:`; body `PUŁAPKA:` `DOBRA PRAKTYKA:`;
   end `ŚCIĄGA:` `PYTANIA KONTROLNE:` `ODPOWIEDZI:`; exercises `ĆWICZENIE N (łatwe|średnie|trudniejsze):`.
   Never `PUŁAPKA 2:`, `ANALOGIA — ...`, `JAK TO DZIAŁA (x):`.
4. Javadoc: lesson class header inside `<pre>...</pre>`; code with `<` `>` `&` → `{@code ...}`; NEVER HTML entities (`&lt;`).
5. Pedagogy: explain WHY, not only HOW; ANALOGIA from everyday life; PRZED/PO (bad → good); every PUŁAPKA and DOBRA PRAKTYKA
   says why; show what Spring does "under the hood" (which bean, which proxy, which SQL, which auto-configuration).
   Short sentences, concrete examples. Mark classic interview questions with the words "PYTANIE REKRUTACYJNE" in the comment text.
6. Cross-references: `sNN_section/ClassName` (this course, registry `tools/lessons.txt`) and `tNN_package/ClassName`
   (JavaLearning, registry `tools/javalearning-lessons.txt`, e.g. `t20_lombok/Lombok03DataValueBuilder`). Only existing names.
7. Every statement must be true for the versions in CLAUDE.md. Use current APIs: `@MockitoBean`/`@MockitoSpyBean` (never
   `@MockBean`), `RestClient`, `JdbcClient`, `ProblemDetail`, Boot 4 starter names (e.g. `spring-boot-starter-webmvc`).
   Java 21 language features are allowed (records, pattern matching for switch, record patterns, virtual threads, text blocks).
   When something changed in Boot 4 / Framework 7 vs older tutorials, say so explicitly (learners will meet old code online).

## 2. Layout of one section (module) — see CLAUDE.md section 2
```
sNN-topic/                                   module (hyphens)
├── pom.xml                                  ONLY the starters this section needs
├── http/<lesson>.http                       ready requests for lessons that run an application
└── src/
    ├── main/java/sNN_topic/                 section package (underscores) + package-info.java (reading order)
    │   ├── xxx01_aspect/                    ONE LESSON = ONE SUBPACKAGE: lesson class Xxx01Aspect + helper classes
    │   │   └── Xxx01Application.java        own @SpringBootApplication ONLY if the lesson needs a Spring context
    │   └── solutions/xxx01_aspect/          reference solutions (sibling of the lessons, never inside a lesson package)
    ├── main/resources/                      minimal application.properties; db/migration/<lesson>/V1__...sql (Flyway)
    └── test/java/sNN_topic/xxx01_aspect/    tests of the lesson and of its exercises; solutions tests under solutions/
```
- NEVER put a `@SpringBootApplication` in the section package itself — it would scan all lessons at once.
- Lesson-specific settings: `@SpringBootTest(properties = ...)`, `@TestPropertySource` or a profile file
  `application-<lesson>.properties` activated by the lesson's application class. H2 per lesson: `jdbc:h2:mem:<lesson>`.
  Flyway per lesson: `spring.flyway.locations=classpath:db/migration/<lesson>`.
- Lesson class name `<Topic><NN><Aspect>` (e.g. `Ioc02ConstructorInjection`), its package `ioc02_constructor_injection`,
  its test `Ioc02ConstructorInjectionTest`.

## 3. Lesson = classes + tests (tests are the "WYNIK")
- Lesson class with the full header, numbered sections (comments + small classes), `ŚCIĄGA:`, `PYTANIA KONTROLNE:` (5–8, at least
  2 of type „Co się stanie?” / „ZNAJDŹ BŁĄD”), and at the very end the collapsed answers:
  `// <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">` … `ODPOWIEDZI:` … `// </editor-fold>`.
- One test class per lesson, one test method per section, `@DisplayName` in Polish describing what the section proves
  ("Kontener wstrzykuje jedyną implementację interfejsu"). AssertJ assertions; failure messages in Polish where helpful.
- Choose the narrowest slice: plain unit test → `@WebMvcTest` + MockMvc → `@DataJpaTest` → `@SpringBootTest`; say in a comment why.
- Answers to „Co się stanie?” questions must be VERIFIED by running code (a test or a scratch run), never computed in your head.

## 4. Exercises (must not break the build)
- Skeleton in the lesson package: methods with `// TODO` that `throw new UnsupportedOperationException("TODO")`.
- Exercise tests annotated `@Tag("cwiczenie")` — excluded from the default `mvnw test`; the learner runs them on purpose.
- Reference solutions in `sNN_topic.solutions.<lesson>`; their tests `@Tag("wzorzec")` run by default and MUST pass.
- 3–5 exercises per lesson, graded łatwe → trudniejsze, each with a hint in Polish.

## 5. Determinism & safety
- Web tests: MockMvc or `@SpringBootTest(webEnvironment = RANDOM_PORT)`; never a fixed port in tests.
- Database: H2 in memory; data via Flyway migrations / `@Sql` / test setup — never depend on test execution order.
- Testcontainers only where the outline says so, and then `@Testcontainers(disabledWithoutDocker = true)` so the build passes
  without Docker. Docker Compose support is for running applications locally only.
- Time via an injected `Clock`; randomness with a seed; no assertions on durations; sort before comparing unordered data.
- No real network in tests (MockRestServiceServer / local stub). No secrets: only obviously fake test keys/passwords.

## 6. Verification (mandatory, after EACH lesson)
1. `./mvnw -q -pl <your-module> test` (Windows: `mvnw.cmd`) — green.
2. `java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/LessonLint.java "<your-module>/**"` — `PROBLEMS: 0`
   (tags, entities, invisible characters, references, registry `tools/lessons.txt`, exercise/solution test tags).
3. Fix everything before starting the next lesson.

## 7. Final report (short)
Files with a 1-line summary each, deviations from the outline (with reason), anything you were unsure about factually,
and the last lines of both verification commands.
