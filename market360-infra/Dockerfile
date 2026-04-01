# ---- Stage 1 : Build Maven ----
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY market360-core/pom.xml market360-core/
COPY market360-agents/pom.xml market360-agents/
COPY market360-skills/pom.xml market360-skills/
COPY market360-api/pom.xml market360-api/
COPY market360-infra/pom.xml market360-infra/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY market360-core/src market360-core/src
COPY market360-agents/src market360-agents/src
COPY market360-skills/src market360-skills/src
COPY market360-api/src market360-api/src

RUN ./mvnw -pl market360-api -am package -DskipTests --no-transfer-progress -q

# ---- Stage 2 : Image finale ----
FROM eclipse-temurin:25-jre

RUN useradd --system --no-create-home market360

COPY --from=build /app/market360-api/target/market360-api-*.jar /app/market360-api.jar

USER market360
EXPOSE 8083

ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/market360-api.jar"]
