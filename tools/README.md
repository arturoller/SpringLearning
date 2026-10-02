# tools — narzędzia autorów kursu

| Plik | Do czego |
|---|---|
| `AGENT_KIT.md` | Zasady pisania lekcji dla podagentów Claude (język, tagi, testy jako „WYNIK”, ćwiczenia, determinizm, weryfikacja). |
| `LessonLint.java` | Sprawdzacz formatu lekcji: tagi, encje HTML, niewidoczne znaki, odwołania do lekcji, rejestr, tagi testów ćwiczeń. |
| `tags.txt` | Lista wymaganych tagów (REQ), tag ćwiczeń (EX) i tagi sprawdzane pod kątem formatu (LINT). |
| `lessons.txt` | Rejestr wszystkich lekcji kursu (`sNN_pakiet/KlasaLekcji`) — tworzony po zatwierdzeniu planu działów. |
| `javalearning-lessons.txt` | Rejestr lekcji kursu JavaLearning — do sprawdzania odesłań `tNN_pakiet/Klasa`. |

Uruchamianie (z dowolnego miejsca w repozytorium):

```
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/LessonLint.java            # cały kurs
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/LessonLint.java "s04*/**"  # wybrany moduł
./mvnw test                                                                              # testy lekcji (Windows: mvnw.cmd)
```

Lekcja jest gotowa, gdy `mvnw test` jest zielony, a `LessonLint` pokazuje `PROBLEMS: 0`.
