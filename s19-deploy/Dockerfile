# syntax=docker/dockerfile:1
# Lekcja s19_deploy / Dep02Docker. Kontekst budowania: katalog s19-deploy (najpierw ./mvnw -pl s19-deploy package).
# Budowanie:  docker build -t shop-deploy:1.0 s19-deploy
# Uruchomienie: docker run --rm -p 8080:8080 shop-deploy:1.0

# ---- Etap 1: ROZPAKOWANIE JAR-a na warstwy (wystarczy JDK, bo korzysta z trybu -Djarmode=tools) ----
FROM eclipse-temurin:25-jdk AS extract
WORKDIR /build
COPY target/s19-deploy-1.0-SNAPSHOT.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- Etap 2: obraz uruchomieniowy — samo JRE, bez narzędzi budowania ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Użytkownik bez uprawnień administratora (nie-root): włamanie do aplikacji nie daje władzy nad kontenerem.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

# Warstwy od najrzadziej zmienianej do najczęściej zmienianej: przy zmianie kodu przebudowuje się tylko ostatnia.
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./

USER app
EXPOSE 8080

# MaxRAMPercentage: JVM bierze 75% pamięci kontenera na stertę (domyślnie tylko 25%), reszta zostaje na stos wątków, metaspace itd.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
