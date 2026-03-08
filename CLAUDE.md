# Gym Tracker

## Vision
An AI-powered workout companion that generates personalized, context-aware workouts. The AI acts as a personal trainer — it knows your goals, remembers your history, adapts to your feedback, and plans intelligently across sessions. No more copying workouts from chat apps. No more generic programs.

See `PRODUCT_PLAN.md` for the full product vision, user flows, and AI strategy.

## Project structure (multi-module monorepo)
```
gym-tracker/
├── shared/       ← KMP module: API contract types + ExerciseCatalog
├── server/       ← JVM module: Ktor backend (owns prompts, calls Claude)
├── composeApp/   ← KMP module: Compose UI app (collects data, displays results)
└── gradle/libs.versions.toml
```

## Stack
- **Kotlin Multiplatform** + **Compose Multiplatform** (shared UI + logic)
- **Ktor 3.0.3** — HTTP client (app) + server (backend)
- Targets: Android, iOS, Web (wasmJs)
- Data layer: in-memory `GymRepository` (SQLDelight planned)
- Navigation: sealed `Screen` class in `App.kt`
- AI: Claude API via Ktor backend server

## Modules

### shared/
API contract types used by both app and server:
- `Equipment`, `ExperienceLevel`, `UserProfile`, `SessionData`, `ExerciseData`, `SetData`
- `GeneratedWorkout`, `GeneratedExercise`, `WeeklySplit`, `SplitDay`
- `WeeklySummary`, `MonthlySummary`
- Request types: `GenerateRequest`, `ResuggestRequest`, `SplitRequest`, etc.
- `ExerciseCatalog` with `MuscleGroup`, `ExerciseCategory`, `CatalogExercise`

### server/
Ktor backend — the "smart" half of the architecture:
- `Application.kt` — Netty server on port 8080, CORS, StatusPages, ContentNegotiation
- `Routes.kt` — POST endpoints: `/workout/generate`, `/workout/resuggest`, `/split/generate`, `/summary/weekly`, `/summary/monthly`, GET `/health`
- `WorkoutService.kt` — orchestrates PromptBuilder → ClaudeClient → JSON parsing
- `PromptBuilder.kt` — all prompt construction logic
- `ClaudeClient.kt` — HTTP transport to Claude API
- `Dockerfile` — Alpine JRE 21 image

### composeApp/
The "dumb" UI — collects data, sends to backend, displays results:
- App-only types in `data/Models.kt`: `WorkoutSession`, `Exercise`, `WorkoutSet`, `WorkoutPlan`
- Typealiases in `data/Models.kt` and `data/ExerciseCatalog.kt` re-export shared types
- `ai/WorkoutAiService.kt` — interface with swappable implementations
- `ai/BackendWorkoutService.kt` — calls server endpoints (production)
- `ai/MockWorkoutService.kt` — local mock (dev/testing)
- `ai/ClaudeWorkoutService.kt` — direct Claude API (MVP/solo)
- `ai/ContextManager.kt` — tiered context assembly (FREE/BASIC/PREMIUM)

## Build
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64

# Compile all modules (metadata check)
GRADLE_OPTS="-Xmx384m -XX:MaxMetaspaceSize=280m -XX:+UseSerialGC" \
  ./gradlew :composeApp:compileCommonMainKotlinMetadata :server:compileKotlin --no-daemon

# Compile web target (needs ~768MB+ heap)
GRADLE_OPTS="-Xmx768m ..." ./gradlew :composeApp:compileKotlinWasmJs --no-daemon

# Run server locally
ANTHROPIC_API_KEY=sk-... ./gradlew :server:run

# Build server fat jar
./gradlew :server:buildFatJar

# If OOM: kill daemons first
./gradlew --stop
```

## Architecture decisions
- **"Dumb app, smart backend"** — app sends structured data, backend owns prompts and calls Claude
- **Multi-module monorepo** — shared types compiled once, no duplication
- **WorkoutAiService interface** — swap implementations without changing UI:
  - `MockWorkoutService` — dev/testing (no API calls)
  - `ClaudeWorkoutService` — calls Claude directly (MVP/solo)
  - `BackendWorkoutService` — calls Ktor server (production)
- **Equipment.NONE** = truly no equipment (push ups, planks)
  - `PULL_UP_BAR`, `DIP_STATION` = needs *something* for that exercise
- **ContextManager** assembles tiered context:
  - FREE: last 3 completed sessions
  - BASIC: + 4 weeks of weekly summaries
  - PREMIUM: + 12 months of monthly trends
- Context built from **completed workouts only** (actuals, not prescriptions)
- Exercise catalog: AI picks from known list for consistent naming and progression tracking

## Roadmap

### Phases 1-4: COMPLETED
- WASM foundations + LlamaEdge local LLM exploration
- User profile + Claude API integration
- Context system + weekly planning
- Multi-module monorepo + Ktor backend

### Phase 5: Polish + production readiness
- SQLDelight persistence (replace in-memory storage)
- Wire real Claude API key
- Post-workout logging with edit-before-complete
- Docker deployment for server

## Conventions
- Kotlin code style: standard Kotlin conventions
- Composables: one screen per file in `ui/` package
- Shared types live in `com.gymtracker.shared` package
- Server-only code in `com.gymtracker.api` and `com.gymtracker.ai` (server module)
