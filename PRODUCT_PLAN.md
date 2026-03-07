# Gym Tracker — Product Plan

## The Problem
Workout apps either give you generic programs or require you to build everything manually. AI chatbots (ChatGPT, Claude) generate great workouts, but you end up copy-pasting exercises into a separate tracking app. The two worlds don't talk to each other.

## The Solution
An app where the AI **is** the workout planner — and it lives inside the tracker. It knows your goal, remembers what you did last week, adjusts when you skip exercises, and learns your preferences over time. One tap to generate today's workout. One tap to swap an exercise. Done.

## Core User Flow

### 1. Onboarding (once)
User sets up their profile:
- **Goal**: "Get bulkier upper body", "Slim down legs", "General fitness", etc.
- **Schedule**: "I plan to work out 4 days a week" (optional — AI adjusts if skipped)
- **Equipment**: Pick from: Barbell, Dumbbell, Cable, Machine, Pull-up Bar, Dip Station, Kettlebell, Band (or none — bodyweight exercises always available)
- **Experience**: Beginner / Intermediate / Advanced
- **Injuries/limitations**: Free text ("bad left knee", "shoulder impingement")

### 2. Weekly Split (automatic)
At the start of each week (or when the user opens the app for a new week), the AI plans a high-level split based on the goal and history:
- "Day 1: Chest/Triceps, Day 2: Back/Biceps, Day 3: Legs, Day 4: Shoulders/Arms"
- This is a suggestion — the user can override on any day
- If the user misses a day, the AI redistributes muscle groups across remaining days

### 3. Session Generation
The user opens the app ready to work out. Two paths:

**Path A — User has a target:**
"I want to focus on glutes today" / "Upper body, 45 minutes"
AI generates a workout considering: target + goal + recent history + weekly split

**Path B — Just go:**
User taps "Generate Workout" with no input.
AI decides what to train based on: weekly split + recent history + goal

The AI returns a full workout plan:
- Exercise names (from a known catalog for consistent tracking)
- Sets, reps, and suggested weight (based on progression from past sessions)
- Estimated duration
- Brief reasoning ("Squats are lighter today since you went heavy 2 days ago")

### 4. Review and Adjust
User sees the generated workout and can:
- **Accept** an exercise (keep it)
- **Reject** an exercise with a reason:
  - "Machine is taken"
  - "Feeling tired for this"
  - "Pain in [area]"
  - "Don't like this exercise"
  - Custom reason
- **Accept all** (skip review)

If any exercises were rejected, the AI re-suggests replacements:
- Keeps all accepted exercises in their positions
- Replaces rejected ones considering the reasons
- Example: "Machine is taken" → suggests a free-weight alternative for the same muscle
- Example: "Pain in left knee" → avoids knee-dominant exercises entirely

This loop can repeat 2-3 times if needed.

### 5. Do the Workout
User works through the exercises. For each set, they see the suggested reps/weight and can:
- Tap to confirm (did as prescribed)
- Edit the actual reps/weight performed
- Skip a set

### 6. Complete Workout
When done, the user taps "Complete Workout." At this point:
- The **actual performed data** (not the prescription) is saved as a completed session
- This completed session becomes part of the AI's context for future workouts
- If the user abandons a workout without completing, it does NOT enter the context

### 7. Repeat
Next session, back to step 3. The AI now knows what you actually did last time.

## Context Architecture

### The Problem with Raw History
Sending every past session as raw data would be expensive and hit token limits fast. A year of 4x/week training is 200+ sessions, each ~150 tokens = 30,000 tokens per request. That's wasteful — no human trainer remembers every rep from 6 months ago either.

### The Solution: Layered Context

```
LAYER 1 — Always sent (every request)
├── User profile (goal, equipment, injuries)      ~80 tokens
├── Current request ("focus on glutes today")      ~30 tokens
├── Last 2-3 completed sessions (full detail)     ~450 tokens
└── Weekly split plan                              ~50 tokens

LAYER 2 — Weekly summaries (basic tier+)
└── Compressed weekly summaries                   ~200 tokens
    "Week of Mar 3: 4 sessions completed. Bench progressed
     80→85kg. Skipped legs once (knee pain). Rejected lunges
     twice — prefers Bulgarian split squats."

LAYER 3 — Monthly trends (premium tier)
└── Compressed monthly summaries                  ~300 tokens
    "January: Focused on hypertrophy. Squat 1RM estimated
     at 100kg (up from 85kg in Dec). Consistently rejects
     overhead press — possible shoulder issue. Prefers
     push/pull/legs split over upper/lower."
```

### Summary Generation
Summaries are AI-generated. Cheap background API calls:
- **Weekly**: After 7 days or 4+ sessions, compress that week's sessions into a ~100-token summary. Cost: ~$0.01
- **Monthly**: At month end, compress 4 weekly summaries into a ~100-token trend. Cost: ~$0.01

### What Summaries Capture
- Progression trends (weights going up/down)
- Exercise preferences (what gets rejected, what's favored)
- Consistency patterns (days skipped, shortened sessions)
- Issues (pain mentions, fatigue patterns, plateau detection)
- Split preferences (what kind of training the user gravitates toward)

## Pricing Model

### Cost Per User (Sonnet)
| Tier | Context sent | AI cost/month | Suggested price |
|------|-------------|---------------|-----------------|
| Free | Last 3 sessions only | ~$0.14 | $0 (trial/ad-supported) |
| Basic | + weekly summaries (1 month) | ~$0.20 | $1/month |
| Premium | + monthly trends (1 year) | ~$0.25 | $3/month |

Note: cost difference between tiers is negligible. The tiers are about perceived value, not cost.

### API Calls Per Session
| Call | When | Tokens | Cost (Sonnet) |
|------|------|--------|---------------|
| Generate workout | Every session | ~1,200 | ~$0.007 |
| Re-suggest | ~60% of sessions | ~1,400 | ~$0.008 |
| Weekly summary | Once/week | ~3,000 | ~$0.01 |
| Weekly split plan | Once/week | ~2,000 | ~$0.008 |
| Monthly summary | Once/month | ~2,000 | ~$0.008 |

Monthly total per user (17 sessions): ~$0.20-0.25

## Exercise Catalog

The AI picks exercises from a curated catalog rather than inventing names freely. This ensures:
- **Consistent naming**: "Bench Press" is always "Bench Press", not sometimes "Flat Barbell Bench"
- **Progression tracking**: The app can chart weight/reps over time per exercise
- **Equipment filtering**: Each exercise is tagged with required equipment

The catalog is expandable. Users can request additions. The AI can suggest new exercises that get added after review.

### Catalog Structure
```
CatalogExercise:
  name: "Barbell Bench Press"
  primaryMuscle: CHEST
  secondaryMuscles: [TRICEPS, SHOULDERS]
  equipment: [BARBELL]          # what the user needs (NONE = no equipment)
  category: COMPOUND
```

Equipment tags indicate what the user needs to have, not a training style:
- `NONE` — truly zero equipment (push ups, planks, burpees)
- `PULL_UP_BAR` — needs something to hang from
- `DIP_STATION` — needs two elevated surfaces
- `BARBELL`, `DUMBBELL`, `CABLE`, `MACHINE`, `KETTLEBELL`, `BAND` — standard gym gear

## Technical Architecture

### AI Service
- Shared KMP code (works on all platforms)
- Ktor HTTP client calling Claude API
- System prompt includes: exercise catalog, JSON schema, user profile
- Response parsing: JSON → data model objects

### Context Manager
- Stores completed sessions locally (SQLDelight)
- Manages summary generation (background API calls)
- Builds the context payload for each request based on user's tier
- Handles the layered context assembly (profile + recent sessions + summaries)

### Workout Flow Manager
- Orchestrates: generate → review → re-suggest → workout → complete
- Tracks which exercises were accepted/rejected
- Handles the "complete" commit point

## Key Design Principles

1. **Actuals over prescriptions** — Context is built from what the user actually did, not what was suggested. The "Complete Workout" button is the commit point.

2. **Graceful degradation** — If the API is down, the user can still manually create workouts and log exercises. AI is an enhancement, not a dependency.

3. **Learn by rejection** — Every rejected exercise with a reason teaches the AI about the user. Over time, it stops suggesting exercises the user consistently rejects.

4. **Smart, not chatty** — The AI returns structured data, not conversational text. Brief reasoning is included but the output is a workout plan, not a paragraph.

5. **Week-aware, not session-blind** — Each workout is part of a weekly plan. The AI ensures balanced training across the week, not just good individual sessions.
