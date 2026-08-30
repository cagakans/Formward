package com.formward.app.score

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class FormScoreResult(
    val dailyScore: Int,
    val weeklyScore: Int,
    val streak: Int
)

internal fun calculateNutritionScore(
    calorieTarget: Double,
    calorieConsumed: Double,
    proteinTarget: Double,
    proteinConsumed: Double,
    waterTarget: Double,
    waterConsumed: Double,
    hasCarbs: Boolean,
    hasFat: Boolean
): Int {
    var score = 0

    if (calorieTarget > 0 && calorieConsumed > 0) {
        val ratio = calorieConsumed / calorieTarget
        score += if (ratio in 0.80..1.10) 12 else 6
    }

    if (proteinTarget > 0 && proteinConsumed > 0) {
        val ratio = proteinConsumed / proteinTarget
        score += if (ratio >= 0.80) 12 else 6
    }

    if (waterTarget > 0 && waterConsumed > 0) {
        val ratio = waterConsumed / waterTarget
        score += if (ratio >= 0.80) 10 else 5
    }

    if (hasCarbs && hasFat) {
        score += 6
    }

    return score.coerceIn(0, 40)
}

internal fun calculateMissionScore(missionScoreRaw: Int): Int {
    return ((missionScoreRaw.coerceIn(0, 100) / 100.0) * 20.0)
        .roundToInt()
        .coerceIn(0, 20)
}

internal fun calculateDailyScore(
    workoutScore: Int,
    nutritionScore: Int,
    missionScore: Int
): Int {
    return (workoutScore + nutritionScore + missionScore)
        .coerceIn(0, 100)
}

fun calculateAndSaveFormScores(context: Context): FormScoreResult {
    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val workoutPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)
    val photoPreferences = context.getSharedPreferences("photo_checkin_data", Context.MODE_PRIVATE)

    val now = Date()

    val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val todayKey = dateKeyFormat.format(now)
    val todayDisplay = displayDateFormat.format(now)
    val todayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(now)

    val missionDate = formPreferences.getString("mission_date", "") ?: ""
    val isTodayMission = missionDate == todayKey

    fun toDouble(value: String): Double {
        return value.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    }

    fun dayKeyFromOffset(offset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        return dateKeyFormat.format(calendar.time)
    }

    fun displayDateFromOffset(offset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        return displayDateFormat.format(calendar.time)
    }

    fun loadDailyScores(): MutableMap<String, Int> {
        val savedData = formPreferences.getString("daily_scores_v1", "") ?: ""

        return savedData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val score = parts[1].toIntOrNull()
                    if (score != null) {
                        parts[0] to score.coerceIn(0, 100)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            .toMap()
            .toMutableMap()
    }

    fun saveDailyScores(scores: Map<String, Int>) {
        val data = scores.entries.joinToString("\n") { entry ->
            "${entry.key}|${entry.value.coerceIn(0, 100)}"
        }

        formPreferences.edit()
            .putString("daily_scores_v1", data)
            .apply()
    }

    // 1) Workout / Recovery: max 40
    val savedWorkoutData = workoutPreferences.getString("day_program_exercises_v1", "") ?: ""
    val workoutHistoryData = workoutPreferences.getString("workout_history_v1", "") ?: ""

    val todayWorkoutExercises = savedWorkoutData
        .lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("|")

            if (parts.size >= 5 && parts[0] == todayName) {
                parts[1]
            } else {
                null
            }
        }

    val completedWorkoutExercisesToday = workoutHistoryData
        .lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("|")

            if (parts.size >= 3) {
                val date = parts[0]
                val dayName = parts[1]
                val exerciseName = parts[2]

                if (date == todayDisplay && dayName == todayName) {
                    exerciseName
                } else {
                    null
                }
            } else {
                null
            }
        }
        .toSet()

    val recoveryDayToday =
        isTodayMission &&
                formPreferences.getBoolean("mission_recovery", false) &&
                todayWorkoutExercises.isEmpty()

    val workoutScore = when {
        todayWorkoutExercises.isNotEmpty() -> {
            val completedCount = todayWorkoutExercises.count { exerciseName ->
                completedWorkoutExercisesToday.contains(exerciseName)
            }

            ((completedCount.toDouble() / todayWorkoutExercises.size.toDouble()) * 40.0)
                .roundToInt()
                .coerceIn(0, 40)
        }

        recoveryDayToday -> 40

        else -> 0
    }

    // 2) Nutrition: max 40
    val nutritionDate = nutritionPreferences.getString("nutrition_date", "") ?: ""
    val isTodayNutrition = nutritionDate == todayKey

    val calorieTarget = toDouble(nutritionPreferences.getString("calorie_target", "") ?: "")
    val calorieConsumed = if (isTodayNutrition) {
        toDouble(nutritionPreferences.getString("calorie_consumed", "") ?: "")
    } else {
        0.0
    }

    val proteinTarget = toDouble(nutritionPreferences.getString("protein_target", "") ?: "")
    val proteinConsumed = if (isTodayNutrition) {
        toDouble(nutritionPreferences.getString("protein_consumed", "") ?: "")
    } else {
        0.0
    }

    val waterTarget = toDouble(nutritionPreferences.getString("water_target", "") ?: "")
    val waterConsumed = if (isTodayNutrition) {
        toDouble(nutritionPreferences.getString("water_consumed", "") ?: "")
    } else {
        0.0
    }

    val carbsConsumed = if (isTodayNutrition) {
        nutritionPreferences.getString("carbs_consumed", "") ?: ""
    } else {
        ""
    }

    val fatConsumed = if (isTodayNutrition) {
        nutritionPreferences.getString("fat_consumed", "") ?: ""
    } else {
        ""
    }

    val nutritionScore = calculateNutritionScore(
        calorieTarget = calorieTarget,
        calorieConsumed = calorieConsumed,
        proteinTarget = proteinTarget,
        proteinConsumed = proteinConsumed,
        waterTarget = waterTarget,
        waterConsumed = waterConsumed,
        hasCarbs = carbsConsumed.isNotBlank(),
        hasFat = fatConsumed.isNotBlank()
    )

    // 3) Daily Missions: max 20
    val missionScoreRaw = if (isTodayMission) {
        formPreferences.getInt("mission_score", 0)
    } else {
        0
    }.coerceIn(0, 100)

    val missionScore = calculateMissionScore(missionScoreRaw)

    // Daily Score: max 100
    val dailyScore = calculateDailyScore(
        workoutScore = workoutScore,
        nutritionScore = nutritionScore,
        missionScore = missionScore
    )

    val dailyScores = loadDailyScores()
    dailyScores[todayKey] = dailyScore
    saveDailyScores(dailyScores)

    // Streak: consecutive days with 60+ daily score
    var streak = 0

    for (i in 0..365) {
        val key = dayKeyFromOffset(i)
        val score = dailyScores[key] ?: 0

        if (score >= 60) {
            streak++
        } else {
            break
        }
    }

    // Weekly Score: average of recorded days in last 7 days + bonuses
    val lastSevenScores = mutableListOf<Int>()

    for (i in 0..6) {
        val key = dayKeyFromOffset(i)
        val score = dailyScores[key]

        if (score != null) {
            lastSevenScores.add(score.coerceIn(0, 100))
        }
    }

    val weeklyBaseScore = if (lastSevenScores.isNotEmpty()) {
        lastSevenScores.average().roundToInt()
    } else {
        0
    }

    val streakBonus = when {
        streak >= 7 -> 10
        streak >= 5 -> 8
        streak >= 3 -> 5
        streak >= 2 -> 3
        else -> 0
    }

    val progressHistoryData = formPreferences.getString("progress_history_v1", "") ?: ""
    val photoEntriesData = photoPreferences.getString("photo_entries", "") ?: ""

    val hasProgressThisWeek = (0..6).any { offset ->
        val date = displayDateFromOffset(offset)

        progressHistoryData
            .lines()
            .filter { it.isNotBlank() }
            .any { line -> line.startsWith(date) }
    }

    val hasPhotoThisWeek = (0..6).any { offset ->
        val date = displayDateFromOffset(offset)

        photoEntriesData
            .lines()
            .filter { it.isNotBlank() }
            .any { line ->
                val parts = line.split("|")
                parts.size == 2 && parts[1].startsWith(date)
            }
    }

    val weeklyProgressBonus =
        (if (hasProgressThisWeek) 3 else 0) +
                (if (hasPhotoThisWeek) 3 else 0)

    val weeklyScore = (
            weeklyBaseScore +
                    streakBonus +
                    weeklyProgressBonus
            ).coerceIn(0, 100)

    formPreferences.edit()
        .putInt("last_score", dailyScore)
        .putInt("today_form_score", dailyScore)
        .putInt("weekly_form_score", weeklyScore)
        .putInt("streak_count", streak)
        .apply()

    return FormScoreResult(
        dailyScore = dailyScore,
        weeklyScore = weeklyScore,
        streak = streak
    )
}