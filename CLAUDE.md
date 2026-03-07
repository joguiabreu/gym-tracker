# Gym Tracker

## Vision
An AI-powered workout companion that generates personalized, context-aware workouts. The AI acts as a personal trainer — it knows your goals, remembers your history, adapts to your feedback, and plans intelligently across sessions. No more copying workouts from chat apps. No more generic programs.

See `PRODUCT_PLAN.md` for the full product vision, user flows, and AI strategy.

## Stack
- **Kotlin Multiplatform** + **Compose Multiplatform** (shared UI + logic)
- Targets: Android, iOS, Web (wasmJs)
- Data layer: in-memory `GymRepository` (SQLDelight planned)
- Navigation: sealed `Screen` class in `App.kt`
- AI: Claude API (Sonnet for production, Haiku for summaries) via OpenAI-compatible HTTP endpoint

## Data model
- `WorkoutSession(id, name, date, exercises, isFinished)` — a completed workout
- `Exercise(id, name, muscleGroup, plannedSets, plannedReps, sets, ...)` — e.g. "Bench Press"
- `WorkoutSet(id, reps, weightKg)` — e.g. 10 reps x 80kg
- `WorkoutPlan(id, name, exercises)` — reusable workout template
- `UserProfile(goal, daysPerWeek, equipment, experience, injuries)` — set during onboarding
- `ExperienceLevel` — BEGINNER, INTERMEDIATE, ADVANCED
- `Equipment` — BARBELL, DUMBBELL, CABLE, MACHINE, NONE, PULL_UP_BAR, DIP_STATION, KETTLEBELL, BAND
- `WeeklySplit(weekStart, days)` — AI-generated weekly training plan
- `SplitDay(dayOfWeek, focus, completed)` — one day in the split
- `WeeklySummary(weekStart, text)` — AI-compressed weekly context
- `MonthlySummary(month, text)` — AI-compressed monthly trends

## Build (from this directory)
```bash
# Always set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64

# Compile web target
./gradlew compileKotlinWasmJs

# Run web dev server
./gradlew wasmJsBrowserDevelopmentRun

# If OOM: kill daemons first
./gradlew --stop
```

## Architecture decisions
- Keep shared logic in `commonMain` — platform code only for platform-specific APIs
- LLM integration lives in shared code, calling an HTTP endpoint (works on all targets)
- Prefer simple, flat code — no premature abstractions
- AI generates workouts via Claude API (Sonnet for generation, Haiku for summaries)
- **WorkoutAiService interface** — implementations are swappable:
  - `MockWorkoutService` — local mock for dev/testing (no API calls)
  - `ClaudeWorkoutService` — calls Claude API directly (MVP/solo use)
  - Future: `BackendWorkoutService` — calls your own backend (production with users)
- `PromptBuilder` lives in shared code — moves to backend when backend is added
- **ContextManager** assembles tiered context for AI prompts:
  - FREE: last 3 completed sessions only
  - BASIC: + 4 weeks of weekly summaries
  - PREMIUM: + 12 months of monthly trend summaries
- Context is built from **completed workouts only** (user-confirmed actuals, not prescriptions)
- Exercise catalog: AI picks from a known list to ensure consistent naming and progression tracking
- Summaries compress history — never send raw session dumps beyond the last few sessions

## WASM learning (completed)
- Phases 1-2 explored WASM runtimes (WasmEdge) and local LLM inference (LlamaEdge)
- Key finding: sub-2B local models can produce JSON but not quality content
- Cloud API (Claude) is the right choice — ~$0.20/month/user with Sonnet
- WasmEdge 0.14.1 remains installed at `~/.wasmedge/` if needed

## Roadmap

### Phase 3: User profile + Claude API integration (COMPLETED)
- ~~UserProfile model + GymRepository storage~~ ✓
- ~~Onboarding screen (profile form + edit profile)~~ ✓
- ~~Ktor HTTP client + ClaudeApiService~~ ✓
- ~~WorkoutGenerator + PromptBuilder + mock responses~~ ✓
- ~~Generate → review → re-suggest UI flow~~ ✓

### Phase 4: Context system + weekly planning (COMPLETED)
- ~~Session completion as context — finished sessions wired into AI prompt with full set data~~ ✓
- ~~Weekly split planning — model, mock generation, HomeScreen display~~ ✓
- ~~Weekly/monthly summary models + generation (mock + Claude)~~ ✓
- ~~ContextManager — tiered context assembly (FREE/BASIC/PREMIUM)~~ ✓
- ~~PromptBuilder enhanced — includes split, weekly summaries, monthly trends~~ ✓

### Phase 5: Polish + production readiness
- SQLDelight persistence (replace in-memory storage)
- Wire real Claude API key (swap MockWorkoutService → ClaudeWorkoutService)
- Post-workout logging with edit-before-complete
- Backend proxy service (when user base grows)

## Conventions
- Kotlin code style: standard Kotlin conventions
- Composables: one screen per file in `ui/` package
- Flag-based CLI syntax where applicable
