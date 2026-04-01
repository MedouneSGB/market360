# Market360

> **Analysez n'importe quel repo GitHub et obtenez un plan marketing complet en quelques minutes.**

Live : **[market360.space](https://market360.space)**

[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.0_GA-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-ai)
[![Claude](https://img.shields.io/badge/Claude-Haiku_4.5-blueviolet?style=flat-square)](https://www.anthropic.com)
[![Cloud Run](https://img.shields.io/badge/Cloud_Run-GCP-4285F4?style=flat-square&logo=googlecloud)](https://cloud.google.com/run)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

---

## Ce que ça fait

Donnez un repo GitHub. Market360 vous retourne en quelques minutes :

- **Analyse produit** — stack, audience, USP, features clés extraits du code et du README
- **Plan GTM** — ICP, positionnement, canaux par ROI, milestones 90 jours, pricing
- **Veille marché** — tendances, concurrents, opportunités, menaces (local ou global)
- **Assets créatifs** — headline, 3 variantes d'ads A/B, brief visuel

```bash
curl -X POST https://market360.space/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/vous/votre-projet","market":"global"}'
# → {"jobId":"uuid"}
```

---

## Architecture

```
CMOAgent  (orchestrateur)
  │
  ├── AnalystAgent      → analyse README, stack, issues, stars
  │
  ├── StrategistAgent   ┐
  ├── TrendsAgent       ┘ parallèle (StructuredTaskScope Java 25)
  │
  └── CreativeAgent     → copy ads + brief visuels
```

Chaque agent est un `@Service` Spring AI avec son propre **skill Markdown** comme system prompt, tournant sur des **virtual threads** (Project Loom).

---

## Stack

| Composant | Choix |
|-----------|-------|
| Language | Java 25 (virtual threads, StructuredTaskScope) |
| Framework | Spring Boot 3.5 + Spring AI 1.0 GA |
| LLM | Claude Haiku 4.5 (Anthropic) |
| LLM local | Ollama `qwen2.5-coder:7b` (dev sans clé API) |
| API | REST + SSE streaming |
| CI/CD | Cloud Build → Cloud Run (auto-deploy sur push) |
| Domaine | market360.space (SSL géré par GCP) |

---

## Démarrage rapide

```bash
git clone https://github.com/MedouneSGB/market360.git
cd market360
cp .env.example .env   # renseigner ANTHROPIC_API_KEY
./mvnw spring-boot:run -pl market360-api
```

Ouvrir [http://localhost:8083](http://localhost:8083) — ou utiliser l'API directement.

**Sans clé API (Ollama) :**
```bash
./mvnw spring-boot:run -pl market360-api -Dspring.profiles.active=local
```

---

## API

### `POST /api/v1/analyze`
```json
{ "repoUrl": "https://github.com/owner/repo", "market": "local|global", "location": "Dakar" }
```
Retourne `{"jobId":"uuid"}` immédiatement (202 Accepted).

### `GET /api/v1/analyze/{jobId}/stream`
Stream SSE temps réel :
```
data: {"agent":"analyst","status":"running","message":"Analyse du repo GitHub..."}
data: {"agent":"all","status":"done","reportId":"uuid"}
```

### `GET /api/v1/analyze/{jobId}/report`
Rapport JSON complet avec `product`, `gtmPlan`, `trends`, `creative`.

---

## Structure

```
market360/
├── market360-core/        # Records, sealed interfaces
├── market360-agents/      # 4 agents IA + orchestrateur
├── market360-skills/      # System prompts Markdown
├── market360-api/         # Spring Boot REST + SSE + UI
└── market360-infra/       # Dockerfile + docker-compose + cloudbuild.yaml
```

---

## Variables d'environnement

| Variable | Requis | Description |
|----------|--------|-------------|
| `ANTHROPIC_API_KEY` | ✅ | console.anthropic.com |
| `GITHUB_TOKEN` | Non | 5000 req/h avec, 60 sans |
| `OLLAMA_BASE_URL` | Non | `http://localhost:11434` |

---

## Roadmap

- [x] Pipeline multi-agents end-to-end
- [x] Streaming SSE temps réel
- [x] UI web avec progression animée
- [x] Déploiement Cloud Run + domaine custom
- [x] CI/CD automatique (Cloud Build)
- [ ] Web search live pour TrendsAgent
- [ ] Génération d'images via fal.ai
- [ ] Persistance PostgreSQL + cache Redis
- [ ] Auth API key

---

<div align="center">

Construit avec Java 25 · Spring AI · Claude · GCP

**[market360.space](https://market360.space)** — Dakar, Sénégal

</div>
