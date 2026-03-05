# Gym Tracker

## Vision
A cross-platform workout tracking app with an integrated LLM that generates workouts on the fly — no more copying from chat apps into workout apps.

## Stack
- **Kotlin Multiplatform** + **Compose Multiplatform** (shared UI + logic)
- Targets: Android, iOS, Web (wasmJs)
- Data layer: in-memory `GymRepository` (SQLDelight planned)
- Navigation: sealed `Screen` class in `App.kt`

## Data model
- `WorkoutSession(id, name, date, exercises)`
- `Exercise(id, name, sets)`
- `WorkoutSet(id, reps, weightKg)`

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
- LLM integration will live in shared code, calling an HTTP endpoint (works on all targets)
- Prefer simple, flat code — no premature abstractions

## WASM + LLM environment

### WasmEdge (installed)
- Version 0.14.1, installed at `~/.wasmedge/`
- Activate with: `source ~/.wasmedge/env`
- WASI-NN plugin included (needed for LLM inference)
- WABT toolkit installed via apt (`wat2wasm`, `wasm2wat`, `wasm-objdump`)

### Key facts for Phase 2
- WASI-NN plugin wraps llama.cpp as a native host plugin — inference runs at native speed, not in WASM
- LlamaEdge's `llama-api-server.wasm` exposes OpenAI-compatible `/v1/chat/completions`
- Target model: SmolLM-360M GGUF (fits in ~600MB free RAM); TinyLlama-1.1B as upgrade if RAM allows
- Container has ~2GB total / ~600MB free — stop Gradle daemons before running LLM

## Roadmap

### Phase 1: WASM foundations — COMPLETED
- WasmEdge runtime installed and verified on arm64
- Explored WASM modules, WASI, host functions

### Phase 2: LlamaEdge — local LLM via WASM (next)
- Download `llama-api-server.wasm` (pre-built API server)
- Download SmolLM-360M GGUF model from HuggingFace
- Start the server, test with curl
- Validate structured JSON output for workout generation

### Phase 3: Workout generation prompt engineering
- System prompt that outputs JSON matching our data model
- Test with SmolLM-360M; try TinyLlama-1.1B if quality is too low
- Goal: reliable structured workout generation from natural language

### Phase 4: Integrate with gym-tracker
- Add Ktor HTTP client to KMP shared code
- `WorkoutGenerator` service that calls the LLM endpoint
- Parse JSON response → WorkoutSession/Exercise/WorkoutSet
- UI: text input → "Generate Workout" → workout created in app

## Conventions
- Kotlin code style: standard Kotlin conventions
- Composables: one screen per file in `ui/` package
- Flag-based CLI syntax where applicable
