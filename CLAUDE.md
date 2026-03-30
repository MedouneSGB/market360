# Market360 — CLAUDE.md

> Constitution permanente du projet. Lire en entier avant toute action.

## Vision produit

Market360 est une équipe IA marketing multi-agents qui analyse un repo GitHub et produit un plan marketing complet prêt à exécuter : analyse produit, plan GTM, tendances marché (local/international), copy ads, et visuels générés — le tout en 10 minutes au lieu de 3 jours.

**Cible** : fondateurs, agences marketing, développeurs indie.
**Modèle économique** : SaaS B2B ($99-299/mois) + pay-per-report ($29-49).

---

## Stack technique

| Composant | Choix | Version |
|-----------|-------|---------|
| Language | Java | 25 LTS |
| Build | Maven | 3.9+ |
| Framework | Spring Boot | 3.5+ |
| AI framework | Spring AI | 1.0 GA |
| LLM principal | Claude Sonnet (Anthropic) | claude-sonnet-4-6 |
| LLM fallback | Ollama (local) | llama3.1 |
| Concurrence | Project Loom virtual threads | Java 25 |
| Persistance | PostgreSQL + Redis | 16 / 7 |
| Déploiement | Cloud Run + GraalVM native image | GCP |
| Tests | JUnit 5 + Testcontainers | latest |

---

## Architecture multi-modules Maven

```
market360/
├── CLAUDE.md                    ← ce fichier
├── pom.xml                      ← parent POM
├── market360-core/              ← modèles, records, interfaces partagées
├── market360-agents/            ← les 4 agents + orchestrateur
├── market360-skills/            ← chargement des skills Markdown
├── market360-api/               ← Spring Boot REST + SSE
└── market360-infra/             ← config GCP, Dockerfile, Cloud Run
```

---

## Agents et responsabilités

```
CMOAgent (orchestrateur)
  ├── AnalystAgent      → clone repo, analyse README/stack/issues/stars
  ├── StrategistAgent   → plan GTM via skills marketing
  ├── TrendsAgent       → tendances web (local ou international)
  └── CreativeAgent     → copy ads + brief visuels → appel fal.ai API
```

Tous les agents tournent en **parallèle** via `StructuredTaskScope` (Java 25 preview).
Chaque agent est un `@Service` Spring avec un `ChatClient` Spring AI injecté.

---

## Conventions de code Java 25

### Toujours utiliser les features modernes

```java
// Records pour les modèles — jamais de classes avec getters/setters
record ProductBrief(String name, String stack, String audience, String usp) {}

// Sealed interfaces pour les états
sealed interface AgentStatus {
    record Running(Instant startedAt)  implements AgentStatus {}
    record Done(Duration elapsed)      implements AgentStatus {}
    record Failed(String reason, Throwable cause) implements AgentStatus {}
}

// Pattern matching exhaustif — pas de default quand ce n'est pas nécessaire
String describe(AgentStatus s) {
    return switch (s) {
        case Running r  -> "En cours depuis " + r.startedAt();
        case Done d     -> "Terminé en " + d.elapsed().toSeconds() + "s";
        case Failed f   -> "Erreur : " + f.reason();
    };
}

// Virtual threads — TOUJOURS pour les opérations I/O
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// StructuredTaskScope pour les agents en parallèle
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var analyst    = scope.fork(() -> analystAgent.analyze(repoUrl));
    var strategist = scope.fork(() -> strategistAgent.buildGTM(context));
    var trends     = scope.fork(() -> trendsAgent.fetch(location));
    var creative   = scope.fork(() -> creativeAgent.generate(brief));
    scope.join().throwIfFailed();
    return MarketReport.from(analyst.get(), strategist.get(), trends.get(), creative.get());
}

// Scoped Values pour propager le contexte request sans ThreadLocal
static final ScopedValue<AnalysisContext> CONTEXT = ScopedValue.newInstance();
```

### Interdictions strictes

- **JAMAIS** de classes mutables avec setters pour les modèles de domaine → Records uniquement
- **JAMAIS** de `new Thread()` ou `ExecutorService.newFixedThreadPool()` pour l'I/O → Virtual threads
- **JAMAIS** de `null` retourné → `Optional<T>` ou sealed result types
- **JAMAIS** de `instanceof` sans pattern matching → `case MyType t ->`
- **JAMAIS** de `ThreadLocal` → `ScopedValue`

---

## Spring AI — configuration agents

Chaque agent suit ce pattern :

```java
@Service
public class AnalystAgent {

    private final ChatClient chatClient;

    public AnalystAgent(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem(SkillLoader.load("repo-analysis"))  // skill Markdown
            .build();
    }

    public ProductBrief analyze(String repoUrl) {
        return chatClient.prompt()
            .user(u -> u.text("Analyse ce repo GitHub : {url}").param("url", repoUrl))
            .call()
            .entity(ProductBrief.class);  // structured output → Record
    }
}
```

**Modèle routing :**
- Agents complexes (Analyst, Strategist) → `claude-sonnet-4-6`
- Agents rapides (Trends, Creative copy) → `claude-haiku-4-5-20251001`
- Fallback si quota → Ollama local `llama3.1`

---

## Skills système

Les skills sont des fichiers Markdown dans `market360-skills/src/main/resources/skills/`.

Skills à créer :
```
skills/
├── repo-analysis.md          ← analyse GitHub repo
├── market360-context.md      ← contexte produit (lu par tous les agents)
├── gtm-strategy.md           ← plan go-to-market
├── geo-market-analysis.md    ← tendances géolocalisées
└── ad-creative.md            ← copy publicitaire
```

Installer aussi depuis `coreyhaines31/marketingskills` :
```bash
npx skills add coreyhaines31/marketingskills --skill \
  product-marketing-context \
  launch-strategy \
  copywriting \
  page-cro \
  ad-creative \
  customer-research \
  competitor-alternatives \
  seo-audit
```

Le `SkillLoader` charge un skill depuis le classpath :
```java
public class SkillLoader {
    public static String load(String skillName) {
        return new ClassPathResource("skills/" + skillName + ".md")
            .getContentAsString(StandardCharsets.UTF_8);
    }
}
```

---

## API REST + SSE streaming

Endpoint principal :
```
POST /api/v1/analyze
Body: { "repoUrl": "https://github.com/...", "market": "local|global", "location": "Dakar" }

GET /api/v1/analyze/{jobId}/stream   → text/event-stream (SSE)
GET /api/v1/analyze/{jobId}/report   → MarketReport JSON complet
GET /api/v1/health                   → Actuator health check
```

Le streaming SSE envoie les updates en temps réel :
```
data: {"agent":"analyst","status":"running","message":"Analyse du repo..."}
data: {"agent":"strategist","status":"running","message":"Construction du plan GTM..."}
data: {"agent":"all","status":"done","reportId":"uuid"}
```

---

## Configuration environnement

Variables d'environnement requises :
```bash
# Anthropic
ANTHROPIC_API_KEY=sk-ant-...          # Obtenir sur console.anthropic.com

# GCP
GOOGLE_CLOUD_PROJECT=market360-prod
GCP_REGION=us-central1

# PostgreSQL (Cloud SQL)
DB_URL=jdbc:postgresql://...
DB_USER=market360
DB_PASSWORD=...

# Redis
REDIS_URL=redis://...

# APIs externes
FAL_API_KEY=...                       # fal.ai pour génération d'images
GITHUB_TOKEN=...                      # GitHub API (éviter rate limiting)
```

Fichier local : `market360-api/src/main/resources/application-local.yml` (gitignored).

---

## GraalVM Native Image

### Build
```bash
# Compiler en native image
./mvnw -Pnative spring-boot:build-image -pl market360-api

# Build Docker multi-stage avec GraalVM
docker build -f market360-infra/Dockerfile -t market360:latest .
```

### Contraintes natives — OBLIGATOIRE
- Toute réflexion → annoter avec `@RegisterReflectionForBinding`
- Les Records Spring AI → ajouter dans `reflect-config.json`
- Tester le build natif régulièrement : `./mvnw -Pnative test`

### Dockerfile (market360-infra/Dockerfile)
```dockerfile
FROM ghcr.io/graalvm/native-image:ol9-java25 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -Pnative -pl market360-api package -DskipTests

FROM debian:12-slim
COPY --from=build /app/market360-api/target/market360-api /market360
EXPOSE 8080
ENTRYPOINT ["/market360"]
```

---

## Tests

```bash
# Lancer tous les tests (Testcontainers démarre PostgreSQL + Redis réels)
./mvnw test

# Tests d'un agent spécifique
./mvnw test -pl market360-agents -Dtest=AnalystAgentTest

# Build natif + tests natifs
./mvnw -Pnative test
```

Convention de nommage :
- `*Test.java` → tests unitaires (pas de container)
- `*IT.java` → tests d'intégration (Testcontainers)

---

## Commandes fréquentes

```bash
# Démarrer en local (mode dev, JVM normale)
./mvnw spring-boot:run -pl market360-api -Dspring.profiles.active=local

# Lancer uniquement PostgreSQL + Redis en Docker pour le dev
docker-compose -f market360-infra/docker-compose-dev.yml up -d

# Déployer sur Cloud Run
gcloud run deploy market360 \
  --image gcr.io/$GOOGLE_CLOUD_PROJECT/market360:latest \
  --region $GCP_REGION \
  --memory 512Mi \
  --cpu 2 \
  --concurrency 80 \
  --set-env-vars ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY

# Vérifier le health
curl https://market360-xxx.run.app/api/v1/health
```

---

## Roadmap de développement

### Phase 1 — MVP (semaines 1-3)
- [ ] Scaffolding multi-modules Maven
- [ ] `AnalystAgent` : analyse repo GitHub via API + README parsing
- [ ] `StrategistAgent` : plan GTM avec skills marketing
- [ ] Endpoint SSE streaming
- [ ] Deploy Cloud Run JVM (avant native)

### Phase 2 — Agents complets (semaines 4-6)
- [ ] `TrendsAgent` : web search local/global
- [ ] `CreativeAgent` : copy ads + appel fal.ai images
- [ ] `CMOAgent` orchestrateur StructuredTaskScope
- [ ] PostgreSQL : persistance des rapports
- [ ] Redis : cache résultats agents

### Phase 3 — Production (semaines 7-9)
- [ ] GraalVM native image + Dockerfile
- [ ] Auth (API key simple pour MVP)
- [ ] Rate limiting (Semaphore virtual threads)
- [ ] Monitoring Actuator + Cloud Monitoring
- [ ] UI minimaliste (ou API-only pour v1)

---

## Ce que Claude Code NE doit PAS faire

- Créer des classes DTO mutables avec getters/setters → toujours des Records
- Utiliser `@Autowired` sur les champs → injection constructeur uniquement
- Ignorer les erreurs des agents → toujours `sealed interface Result<T>`
- Committer les secrets → toutes les clés via variables d'environnement
- Utiliser `Thread.sleep()` → `ScheduledExecutorService` avec virtual threads
- Créer des endpoints sans test d'intégration

