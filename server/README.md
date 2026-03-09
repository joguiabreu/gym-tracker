# Gym Tracker Server

Ktor backend that orchestrates AI-powered workout generation. It builds prompts, calls Claude, and returns structured workout JSON. The app (Compose UI) sends user data here and displays the results.

## Quick Start

```bash
# From the gym-tracker/ root directory:
./gradlew :server:run
```

That's it. The server starts on `http://localhost:8080` in **mock mode** by default — no API key, no external calls, instant fake responses using real exercises from the catalog.

## Modes

The server has two modes, controlled by the `GYM_MODE` environment variable:

| Mode | `GYM_MODE` | API Key Required | What Happens |
|------|-----------|------------------|-------------|
| **Mock** (default) | `mock` or unset | No | Returns realistic fake responses instantly. Uses real exercise names from the catalog. All logging and JSON parsing runs identically to production. |
| **Live** | `live` | Yes | Calls Claude API for real AI-generated workouts. Requires `ANTHROPIC_API_KEY`. |

### Running in mock mode (development/testing)

```bash
./gradlew :server:run
```

No environment variables needed. The server defaults to mock mode via `application.conf`. Use this for:
- Testing the API contract with Postman or curl
- Frontend development without API costs
- CI/CD pipeline testing
- Verifying observability (all logging works identically)

### Running in live mode (production)

```bash
GYM_MODE=live ANTHROPIC_API_KEY=sk-ant-... ./gradlew :server:run
```

Optionally override the Claude model:

```bash
GYM_MODE=live ANTHROPIC_API_KEY=sk-ant-... CLAUDE_MODEL=claude-opus-4-20250514 ./gradlew :server:run
```

## Configuration

All config lives in `src/main/resources/application.conf` (HOCON format). Defaults are tuned for development. Override any value with environment variables:

| Setting | Config Key | Env Var | Default |
|---------|-----------|---------|---------|
| Server port | `ktor.deployment.port` | `PORT` | `8080` |
| Mode | `gymtracker.mode` | `GYM_MODE` | `mock` |
| Claude model | `gymtracker.claude.model` | `CLAUDE_MODEL` | `claude-sonnet-4-20250514` |
| API key | `gymtracker.claude.apiKey` | `ANTHROPIC_API_KEY` | (empty) |

## API Endpoints

Base URL: `http://localhost:8080`

All POST endpoints accept and return `application/json`.

### `GET /health`

Health check. Returns the current mode.

```
GET http://localhost:8080/health
→ OK (mode=mock)
```

### `POST /workout/generate`

Generate a workout based on the user's profile and optional target muscle group.

```bash
curl -X POST http://localhost:8080/workout/generate \
  -H "Content-Type: application/json" \
  -d '{
    "profile": {
      "goal": "Get bulkier upper body",
      "days_per_week": 4,
      "equipment": ["BARBELL", "DUMBBELL", "CABLE"],
      "experience": "INTERMEDIATE"
    },
    "target": "chest"
  }'
```

**Request body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `profile` | `UserProfile` | Yes | User's profile (see below) |
| `target` | `string` | No | Target muscle/focus (e.g. "chest", "legs", "upper body 45 minutes"). Empty = AI decides. |
| `recent_sessions` | `SessionData[]` | No | Last 1-3 completed sessions for context |
| `weekly_summaries` | `WeeklySummary[]` | No | Compressed weekly summaries (Basic tier) |
| `monthly_summaries` | `MonthlySummary[]` | No | Compressed monthly trends (Premium tier) |

**Response:** `GeneratedWorkout` — list of exercises with sets, reps, suggested weight, and reasoning.

### `POST /workout/resuggest`

Replace rejected exercises while keeping accepted ones.

```bash
curl -X POST http://localhost:8080/workout/resuggest \
  -H "Content-Type: application/json" \
  -d '{
    "profile": {
      "goal": "Get bulkier upper body",
      "days_per_week": 4,
      "equipment": ["BARBELL", "DUMBBELL"],
      "experience": "INTERMEDIATE"
    },
    "kept": [
      {"name": "Barbell Bench Press", "planned_sets": 4, "planned_reps": 8, "suggested_weight_kg": 60.0}
    ],
    "rejected": [
      {"name": "Pec Deck", "planned_sets": 3, "planned_reps": 12, "reason": "Machine is taken"},
      {"name": "Tricep Pushdown", "planned_sets": 3, "planned_reps": 12, "reason": "Feeling tired"}
    ]
  }'
```

**Response:** `GeneratedWorkout` — kept exercises preserved, rejected ones replaced.

### `POST /split/generate`

Generate a weekly training split.

```bash
curl -X POST http://localhost:8080/split/generate \
  -H "Content-Type: application/json" \
  -d '{
    "profile": {
      "goal": "General fitness",
      "days_per_week": 4,
      "equipment": ["DUMBBELL", "BARBELL"],
      "experience": "BEGINNER"
    }
  }'
```

**Response:** `WeeklySplit` — 7 days with focus areas and rest days.

### `POST /summary/weekly`

Compress completed sessions into a weekly summary.

```bash
curl -X POST http://localhost:8080/summary/weekly \
  -H "Content-Type: application/json" \
  -d '{
    "sessions": [
      {
        "date": "2026-03-03",
        "exercises": [
          {"name": "Bench Press", "muscle_group": "chest", "sets": [
            {"reps": 8, "weight_kg": 60}, {"reps": 8, "weight_kg": 65}, {"reps": 6, "weight_kg": 65}
          ]},
          {"name": "Incline DB Press", "muscle_group": "chest", "sets": [
            {"reps": 10, "weight_kg": 24}, {"reps": 10, "weight_kg": 24}
          ]}
        ]
      },
      {
        "date": "2026-03-05",
        "exercises": [
          {"name": "Squat", "muscle_group": "quads", "sets": [
            {"reps": 5, "weight_kg": 80}, {"reps": 5, "weight_kg": 85}
          ]}
        ]
      }
    ]
  }'
```

**Response:** `WeeklySummary` — `{ "week_start": "...", "text": "..." }`

### `POST /summary/monthly`

Compress weekly summaries into a monthly trend report.

```bash
curl -X POST http://localhost:8080/summary/monthly \
  -H "Content-Type: application/json" \
  -d '{
    "weekly_summaries": [
      {"week_start": "2026-03-03", "text": "4 sessions. Bench 60→65kg. Good consistency."},
      {"week_start": "2026-03-10", "text": "3 sessions. Squat volume up. Skipped Friday."}
    ],
    "month": "2026-03"
  }'
```

**Response:** `MonthlySummary` — `{ "month": "...", "text": "..." }`

## Shared Types

### `UserProfile`

```json
{
  "goal": "Get bulkier upper body",
  "days_per_week": 4,
  "equipment": ["BARBELL", "DUMBBELL", "CABLE"],
  "experience": "INTERMEDIATE",
  "injuries": "bad left knee"
}
```

- `equipment` values: `BARBELL`, `DUMBBELL`, `CABLE`, `MACHINE`, `NONE`, `PULL_UP_BAR`, `DIP_STATION`, `KETTLEBELL`, `BAND`
- `experience` values: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- `injuries` is optional free text

### `GeneratedWorkout` (response)

```json
{
  "exercises": [
    {
      "name": "Barbell Bench Press",
      "planned_sets": 4,
      "planned_reps": 8,
      "suggested_weight_kg": 60.0,
      "reason": "Primary chest compound — progressing from last session's 57.5kg"
    }
  ],
  "reasoning": "Chest-focused session with tricep accessories to complement your push day"
}
```

## Observability

The server logs at two levels:

- **INFO** — Every request: what was asked, timing, result summary. Always visible.
- **DEBUG** — Full prompt text, raw Claude responses, token usage. Visible in dev (configured in `logback.xml`).

Log config: `src/main/resources/logback.xml`. The `com.gymtracker` logger is set to `DEBUG` by default.

## Build & Deploy

```bash
# Compile only
./gradlew :server:compileKotlin

# Build fat JAR for deployment
./gradlew :server:buildFatJar

# Run fat JAR directly
GYM_MODE=live ANTHROPIC_API_KEY=sk-ant-... java -jar server/build/libs/server-all.jar

# Docker
docker build -t gym-tracker-server server/
docker run -p 8080:8080 -e GYM_MODE=live -e ANTHROPIC_API_KEY=sk-ant-... gym-tracker-server
```

## Testing with the Compose App

The Compose app (`composeApp/`) has its own mock service for UI development:

```kotlin
// In App.kt — swap the service to test different backends:
val aiService = remember { LoggingWorkoutService(MockWorkoutService()) }           // mock (default)
val aiService = remember { LoggingWorkoutService(BackendWorkoutService(baseUrl)) } // server
val aiService = remember { LoggingWorkoutService(ClaudeWorkoutService(apiKey)) }   // direct API
```

To test the app against the server, start the server in mock mode and point `BackendWorkoutService` at `http://localhost:8080`.
