# AGENT KIT (SpringLearning) — everything a sub-agent needs to write lessons (read ONLY this file + your outline)

The main session gives you: this kit + ONE outline section from `temp/ASSIGNMENTS.md` (or the text of it in the prompt).
Do not explore the repository beyond what your outline names. Never run environment-check commands (`java -version`, `ls`...).
Project-level decisions (Spring Boot / Java versions, module layout, build commands) live in `CLAUDE.md` — read its sections
about structure and versions if your outline does not repeat them.

## 1. Rules
1. Create/edit ONLY the files of your assignment (your module / package). No git. No sub-agents. Never edit other lessons,
   README.md, CLAUDE.md, the parent pom or shared configuration unless your outline says so.
2. Audience: a Polish learner with weak English. ALL comments, test display names and printed text in Polish with diacritics;
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
   says why; show what Spring does "under the hood" (which bean, which proxy, which SQL). Short sentences, concrete examples.
   Mark classic interview questions with the words "PYTANIE REKRUTACYJNE" inside the comment text (not as a tag).
6. Cross-references: `sNN_package/ClassName` (this course, registry `tools/lessons.txt`) and `tNN_package/ClassName`
   (JavaLearning, registry `tools/javalearning-lessons.txt`, e.g. `t20_lombok/Lombok03DataValueBuilder`). Only existing names.
7. Every statement must be true for the Spring Boot / Java versions in CLAUDE.md. Prefer current APIs (e.g. `@MockitoBean`,
   `RestClient`, `JdbcClient`); when something is deprecated or changed in recent versions, say so explicitly.

## 2. Lesson = classes + tests (tests are the "WYNIK")
- Lesson class `<Topic><NN><Aspect>` (e.g. `Ioc02ConstructorInjection`) with the full header, numbered sections as comments
  and/or small nested/package classes, `ŚCIĄGA:`, `PYTANIA KONTROLNE:` (5–8, at least 2 of type „Co się stanie?” / „ZNAJDŹ BŁĄD”),
  and at the very end the collapsed answers:
  `// <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">` … `ODPOWIEDZI:` … `// </editor-fold>`.
- One test class per lesson (`…Test`), one test method per section, `@DisplayName` in Polish describing what the section proves
  ("Kontener wstrzykuje jedyną implementację interfejsu"). Assertions with AssertJ or JUnit; messages in Polish.
- Choose the narrowest test slice: plain unit test → `@WebMvcTest` + MockMvc → `@DataJpaTest` → `@SpringBootTest`
  (and explain in a comment why that slice).
- Answers to „Co się stanie?” questions must be VERIFIED by running code (a test or a scratch run), never computed in your head.

## 3. Exercises (must not break the build)
- Skeleton in main code: methods with `// TODO` that `throw new UnsupportedOperationException("TODO")`.
- Exercise tests annotated `@Tag("cwiczenie")` — excluded from the default `mvnw test`; the learner runs them on purpose.
- Reference solutions in a `solutions` sub-package; their tests `@Tag("wzorzec")` run by default and MUST pass.
- 3–5 exercises per lesson, graded łatwe → trudniejsze, each with a hint in Polish.

## 4. Determinism & safety
- Web tests: MockMvc or `@SpringBootTest(webEnvironment = RANDOM_PORT)`; never a fixed port in tests.
- Database: H2 in memory (or Testcontainers where the outline says so — then `@Testcontainers(disabledWithoutDocker = true)`
  so the build passes without Docker). Data via Flyway migrations / `@Sql` / test setup — never depend on test order.
- Time via an injected `Clock`; randomness with a seed; no assertions on durations; sort before comparing unordered data.
- No real network in tests (use MockRestServiceServer / a local stub). No secrets: only obviously fake test keys/passwords.

## 5. Verification (mandatory, after EACH lesson)
1. `./mvnw -q -pl <your-module> test` (Windows: `mvnw.cmd`) — green.
2. `java tools/LessonLint.java` — `PROBLEMS: 0` for your files (it checks tags, entities, invisible characters, references,
   registry and exercise tags).
3. Fix everything before starting the next lesson.

## 6. Final report (short)
Files with a 1-line summary each, deviations from the outline (with reason), anything you were unsure about factually,
and the last lines of both verification commands.
