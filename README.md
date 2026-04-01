# Market360 ⚡

> **Une équipe IA marketing complète qui analyse votre repo GitHub et produit un plan marketing prêt à exécuter — en 10 minutes au lieu de 3 jours.**

🚀 **Production** : `https://market360-878412550299.us-central1.run.app`

[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.0_GA-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-ai)
[![Claude](https://img.shields.io/badge/Claude-Sonnet_4.6-blueviolet?style=flat-square)](https://www.anthropic.com)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

---

## Ce que ça fait

Vous donnez un repo GitHub. Market360 vous retourne en quelques minutes :

- **Analyse produit** — stack, audience, USP, features clés extraits du code et du README
- **Plan GTM complet** — ICP, positionnement, canaux par ROI, milestones 90 jours, pricing
- **Veille marché** — tendances, concurrents, opportunités, menaces (local ou global)
- **Assets créatifs** — headline, 3 variantes d'ads A/B, brief visuel pour génération d'images

```bash
curl -X POST https://market360-878412550299.us-central1.run.app/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/vous/votre-projet","market":"global"}'

# → {"jobId":"uuid"}
# → SSE stream temps réel
# → Rapport JSON complet
```

---

## Architecture multi-agents

```
CMOAgent  (orchestrateur)
  │
  ├─ Phase 1 ──▶  AnalystAgent      → clone repo, analyse README + issues + stars
  │
  ├─ Phase 2 ──▶  StrategistAgent   → plan GTM via skills marketing       ┐ parallèle
  │          └──▶  TrendsAgent       → tendances web local / international  ┘ (StructuredTaskScope)
  │
  └─ Phase 3 ──▶  CreativeAgent     → copy ads + brief visuels
```

Chaque agent est un `@Service` Spring AI avec son propre **skill Markdown** comme system prompt. Tous tournent sur des **virtual threads** (Project Loom, Java 25).

---

## Stack technique

| Composant | Choix |
|-----------|-------|
| Language | Java 25 (preview features — StructuredTaskScope) |
| Framework | Spring Boot 3.5 + Spring AI 1.0 GA |
| LLM principal | Claude Sonnet 4.6 (Anthropic) |
| LLM local | Ollama `qwen2.5-coder:7b` (dev sans clé API) |
| Concurrence | Virtual Threads + StructuredTaskScope |
| API | REST + SSE streaming |
| Déploiement | GraalVM native image + Cloud Run |
| Tests | JUnit 5 + Testcontainers |

---

## Démarrage rapide

### Prérequis
- Java 25+
- Maven 3.9+
- Clé API [Anthropic](https://console.anthropic.com) — ou Ollama en local

### 1. Cloner

```bash
git clone https://github.com/MedouneSGB/market360.git
cd market360
```

### 2. Configurer

```bash
cp .env.example .env
# Éditez .env et renseignez votre ANTHROPIC_API_KEY
```

### 3. Lancer

```bash
# Avec Claude (Anthropic)
export $(grep -v '^#' .env | xargs)
export JAVA_HOME=/path/to/jdk-25
./mvnw spring-boot:run -pl market360-api

# Avec Ollama (sans clé API)
./mvnw spring-boot:run -pl market360-api -Dspring.profiles.active=local
```

### 4. Tester

```bash
# Lancer une analyse
curl -X POST http://localhost:8083/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/MedouneSGB/flash-transfer","market":"global"}'

# Suivre en temps réel (SSE)
curl -N http://localhost:8083/api/v1/analyze/{jobId}/stream

# Récupérer le rapport final
curl http://localhost:8083/api/v1/analyze/{jobId}/report
```

---

## API Reference

### `POST /api/v1/analyze`

Lance une analyse asynchrone. Retourne un `jobId` immédiatement (202 Accepted).

```json
{
  "repoUrl": "https://github.com/owner/repo",
  "market": "global",
  "location": "Dakar"
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `repoUrl` | string | ✅ | URL GitHub valide |
| `market` | `local` \| `global` | ✅ | Scope de l'analyse marché |
| `location` | string | si `local` | Ville ou pays |

### `GET /api/v1/analyze/{jobId}/stream`

Stream SSE temps réel de la progression.

```
data: {"agent":"analyst","status":"running","message":"Analyse du repo GitHub..."}
data: {"agent":"analyst","status":"done","message":"Analyse terminée"}
data: {"agent":"strategist","status":"running","message":"Construction du plan GTM..."}
data: {"agent":"trends","status":"running","message":"Analyse des tendances marché..."}
...
data: {"agent":"all","status":"done","reportId":"uuid"}
```

### `GET /api/v1/analyze/{jobId}/report`

Rapport marketing complet en JSON.

```json
{
  "id": "uuid",
  "generatedAt": "2026-03-31T...",
  "product": {
    "name": "Flash Transfer",
    "stack": "JavaScript, Rust, Tauri 2, WebRTC",
    "usp": "Transfert P2P à >400 MB/s, sans cloud, sans compte",
    "keyFeatures": ["..."]
  },
  "gtmPlan": {
    "targetSegment": "...",
    "positioningStatement": "...",
    "channels": ["..."],
    "milestones": ["..."],
    "pricingStrategy": "..."
  },
  "trends": {
    "trends": ["..."],
    "competitors": ["..."],
    "opportunities": ["..."],
    "threats": ["..."]
  },
  "creative": {
    "headline": "Transfer Files at 400 MB/s. No Cloud. No Account.",
    "adVariants": ["..."],
    "visualBrief": "..."
  }
}
```

---

## Structure du projet

```
market360/
├── market360-core/        # Records immuables, sealed interfaces
├── market360-agents/      # Les 4 agents IA + orchestrateur
├── market360-skills/      # System prompts en Markdown
├── market360-api/         # Spring Boot REST + SSE
└── market360-infra/       # Dockerfile GraalVM + docker-compose
```

### Skills Markdown

Les agents utilisent des fichiers Markdown comme system prompts, versionnés avec le code :

| Skill | Agent | Rôle |
|-------|-------|------|
| `repo-analysis.md` | AnalystAgent | Senior Technical Analyst |
| `gtm-strategy.md` | StrategistAgent | Chief Marketing Strategist |
| `geo-market-analysis.md` | TrendsAgent | Market Intelligence Analyst |
| `ad-creative.md` | CreativeAgent | Creative Director & Copywriter |
| `senegal-market-study.md` | TrendsAgent | Étude de marché Sénégal 2025 |

---

## Conventions Java 25

Ce projet utilise les features modernes de Java 25 :

```java
// Records pour tous les modèles de domaine
record ProductBrief(String name, String stack, String audience, String usp) {}

// Sealed interfaces pour les états d'agents
sealed interface AgentStatus {
    record Running(Instant startedAt) implements AgentStatus {}
    record Done(Duration elapsed)     implements AgentStatus {}
    record Failed(String reason)      implements AgentStatus {}
}

// StructuredTaskScope pour la parallélisation des agents
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var gtm    = scope.fork(() -> strategistAgent.buildGTM(brief));
    var trends = scope.fork(() -> trendsAgent.fetch(brief, market, location));
    scope.join().throwIfFailed();
    // gtm.get() et trends.get() disponibles
}
```

---

## Déploiement

### Docker (JVM)

```bash
docker build -f market360-infra/Dockerfile -t market360:latest .
docker run -p 8083:8083 -e ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY market360:latest
```

### GraalVM Native Image

```bash
./mvnw -Pnative spring-boot:build-image -pl market360-api
```

### Google Cloud Run

```bash
gcloud run deploy market360 \
  --image gcr.io/$GOOGLE_CLOUD_PROJECT/market360:latest \
  --region us-central1 \
  --memory 512Mi --cpu 2 \
  --set-env-vars ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY
```

---

## Roadmap

- [x] Pipeline multi-agents end-to-end (Analyst → Strategist → Trends → Creative)
- [x] Streaming SSE temps réel
- [x] Support local/global avec données Sénégal
- [x] Fallback Ollama local (sans clé API)
- [x] Déploiement Cloud Run (GCP)
- [ ] `TrendsAgent` — intégration web search live
- [ ] `CreativeAgent` — génération d'images via fal.ai
- [ ] Persistance PostgreSQL + cache Redis
- [ ] Auth API key simple
- [ ] GraalVM native image + déploiement Cloud Run
- [ ] UI minimaliste

---

## Variables d'environnement

| Variable | Requis | Description |
|----------|--------|-------------|
| `ANTHROPIC_API_KEY` | ✅ | Clé API Anthropic (console.anthropic.com) |
| `GITHUB_TOKEN` | Non | Token GitHub — 5000 req/h avec, 60 sans |
| `OLLAMA_BASE_URL` | Non | URL Ollama local (défaut: `http://localhost:11434`) |
| `OLLAMA_MODEL` | Non | Modèle Ollama (défaut: `qwen2.5-coder:7b`) |

---

## Contribuer

```bash
# Tests unitaires
./mvnw test -pl market360-agents

# Tests d'intégration (Testcontainers)
./mvnw test -pl market360-api

# Health check
curl http://localhost:8083/actuator/health
```

---

<div align="center">

Construit avec ☕ Java 25 · Spring AI · Claude Sonnet 4.6

**[Fondateurs](https://github.com/MedouneSGB)** · Dakar, Sénégal

</div>
