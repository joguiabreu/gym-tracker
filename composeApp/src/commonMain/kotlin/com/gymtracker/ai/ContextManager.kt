package com.gymtracker.ai

import com.gymtracker.data.*

enum class UserTier { FREE, BASIC, PREMIUM }

data class WorkoutContext(
    val profile: UserProfile,
    val recentSessions: List<WorkoutSession>,
    val weeklySplit: WeeklySplit?,
    val weeklySummaries: List<WeeklySummary>,
    val monthlySummaries: List<MonthlySummary>
)

class ContextManager(
    private val repository: GymRepository,
    private val tier: UserTier = UserTier.FREE
) {
    fun buildContext(): WorkoutContext {
        val profile = repository.getProfile()
            ?: throw IllegalStateException("No user profile")

        val finished = repository.getFinishedSessions()

        val recentSessions = when (tier) {
            UserTier.FREE -> finished.takeLast(3)
            UserTier.BASIC -> finished.takeLast(3)
            UserTier.PREMIUM -> finished.takeLast(3)
        }

        val weeklySummaries = when (tier) {
            UserTier.FREE -> emptyList()
            UserTier.BASIC -> repository.getWeeklySummaries().takeLast(4)
            UserTier.PREMIUM -> repository.getWeeklySummaries().takeLast(4)
        }

        val monthlySummaries = when (tier) {
            UserTier.FREE -> emptyList()
            UserTier.BASIC -> emptyList()
            UserTier.PREMIUM -> repository.getMonthlySummaries().takeLast(12)
        }

        return WorkoutContext(
            profile = profile,
            recentSessions = recentSessions,
            weeklySplit = repository.getCurrentSplit(),
            weeklySummaries = weeklySummaries,
            monthlySummaries = monthlySummaries
        )
    }

    fun estimateTokens(): Int {
        val context = buildContext()
        var tokens = 80  // profile
        tokens += 30     // current request
        tokens += context.recentSessions.size * 150  // ~150 per session
        if (context.weeklySplit != null) tokens += 50
        tokens += context.weeklySummaries.size * 50   // ~50 per weekly summary
        tokens += context.monthlySummaries.size * 25   // ~25 per monthly summary
        return tokens
    }
}
