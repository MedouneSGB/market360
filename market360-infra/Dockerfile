# ---- Stage 1 : GraalVM native image build ----
FROM ghcr.io/graalvm/native-image:ol9-java25 AS build

WORKDIR /app
COPY . .
RUN ./mvnw -Pnative -pl market360-api package -DskipTests --no-transfer-progress

# ---- Stage 2 : Image finale minimale ----
FROM debian:12-slim

RUN useradd --system --no-create-home market360

COPY --from=build /app/market360-api/target/market360-api /market360
RUN chmod +x /market360

USER market360
EXPOSE 8080

ENTRYPOINT ["/market360"]
