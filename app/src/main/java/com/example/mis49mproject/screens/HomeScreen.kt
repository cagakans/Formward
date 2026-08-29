package com.example.mis49mproject.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.mis49mproject.R


@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onMissionClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onNutritionClick: () -> Unit,
    onProgressClick: () -> Unit
) {
    val context = LocalContext.current

    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val workoutPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)

    val todayFormScore = formPreferences.getInt("today_form_score", 0)
    val weeklyFormScore = formPreferences.getInt("weekly_form_score", 0)
    val streakCount = formPreferences.getInt("streak_count", 0)

    val weight = formPreferences.getString("weight", "-") ?: "-"
    val latestBodyFat = formPreferences.getString("latest_body_fat", "-") ?: "-"

    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val todayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())

    fun toDouble(value: String): Double {
        return value.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    }

    // Workout summary
    val savedWorkoutData = workoutPreferences.getString("day_program_exercises_v1", "") ?: ""
    val workoutHistoryData = workoutPreferences.getString("workout_history_v1", "") ?: ""

    val todayExercises = savedWorkoutData
        .lines()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 5 && parts[0] == todayName) parts[1] else null
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

                if (date == todayDisplay && dayName == todayName) exerciseName else null
            } else {
                null
            }
        }
        .toSet()

    val completedToday = todayExercises.count { exerciseName ->
        completedWorkoutExercisesToday.contains(exerciseName)
    }

    val totalToday = todayExercises.size

    val workoutStatus = if (totalToday > 0) {
        "$completedToday / $totalToday completed"
    } else {
        "No workout planned"
    }

    val workoutProgress = if (totalToday > 0) {
        completedToday.toFloat() / totalToday.toFloat()
    } else {
        0f
    }

    // Mission summary
    val missionDate = formPreferences.getString("mission_date", "") ?: ""
    val isTodayMission = missionDate == todayKey
    val hasWorkoutPlanToday = totalToday > 0

    val missionWorkout = isTodayMission && hasWorkoutPlanToday &&
            formPreferences.getBoolean("mission_workout", false)

    val missionRecovery = isTodayMission && !hasWorkoutPlanToday &&
            formPreferences.getBoolean("mission_recovery", false)

    val missionNutrition = isTodayMission &&
            formPreferences.getBoolean("mission_nutrition", false)

    val missionWater = isTodayMission &&
            formPreferences.getBoolean("mission_water", false)

    val missionMovement = isTodayMission &&
            formPreferences.getBoolean(
                "mission_movement",
                formPreferences.getBoolean("mission_steps", false)
            )

    val missionSleep = isTodayMission &&
            formPreferences.getBoolean("mission_sleep", false)

    val missionCompletedCount = listOf(
        missionWorkout || missionRecovery,
        missionNutrition,
        missionWater,
        missionMovement,
        missionSleep
    ).count { it }

    val missionProgress = missionCompletedCount / 5f

    // Nutrition summary
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

    var nutritionContribution = 0

    if (calorieTarget > 0 && calorieConsumed > 0) {
        val ratio = calorieConsumed / calorieTarget
        nutritionContribution += if (ratio in 0.80..1.10) 12 else 6
    }

    if (proteinTarget > 0 && proteinConsumed > 0) {
        val ratio = proteinConsumed / proteinTarget
        nutritionContribution += if (ratio >= 0.80) 12 else 6
    }

    if (waterTarget > 0 && waterConsumed > 0) {
        val ratio = waterConsumed / waterTarget
        nutritionContribution += if (ratio >= 0.80) 10 else 5
    }

    if (carbsConsumed.isNotBlank() && fatConsumed.isNotBlank()) {
        nutritionContribution += 6
    }

    nutritionContribution = nutritionContribution.coerceIn(0, 40)

    val hasNutritionTargets =
        calorieTarget > 0 &&
                proteinTarget > 0 &&
                waterTarget > 0

    val hasNutritionLogToday =
        isTodayNutrition &&
                (
                        calorieConsumed > 0 ||
                                proteinConsumed > 0 ||
                                waterConsumed > 0 ||
                                carbsConsumed.isNotBlank() ||
                                fatConsumed.isNotBlank()
                        )

    val nutritionCardValue = when {
        !hasNutritionTargets -> "Set targets first"
        !hasNutritionLogToday -> "No intake logged"
        else -> "$nutritionContribution / 40"
    }

    val nutritionCardDescription = when {
        !hasNutritionTargets -> "Calculate your daily targets."
        !hasNutritionLogToday -> "Log calories, protein, water and macros."
        else -> "Today's intake is logged."
    }

    val nutritionProgress = if (hasNutritionTargets && hasNutritionLogToday) {
        nutritionContribution / 40f
    } else {
        0f
    }

    val scoreMessage = when {
        todayFormScore >= 85 -> "Elite day"
        todayFormScore >= 60 -> "Strong momentum"
        todayFormScore > 0 -> "Keep building discipline"
        else -> "Start your day"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Formward",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(7.dp))

            Image(
                painter = painterResource(id = R.drawable.formward_logo),
                contentDescription = "Formward Logo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "Forward Your Form",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Score Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Form Score",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = "$todayFormScore",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Open Your Profile →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = "$scoreMessage • $todayFormScore / 100",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(7.dp))

                LinearProgressIndicator(
                    progress = { todayFormScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(7.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MiniStat(
                        title = "Weekly",
                        value = "$weeklyFormScore / 100",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    MiniStat(
                        title = "Streak",
                        value = "$streakCount ${if (streakCount == 1) "day" else "days"}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CompactHomeCard(
            title = "Daily Mission",
            value = "$missionCompletedCount / 5",
            description = "Complete today's checklist.",
            icon = Icons.Filled.CheckCircle,
            progress = missionProgress,
            buttonText = "Open Mission →",
            onClick = onMissionClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        CompactHomeCard(
            title = "Today's Workout",
            value = workoutStatus,
            description = todayName,
            icon = Icons.Filled.FitnessCenter,
            progress = workoutProgress,
            buttonText = "Open Workout →",
            onClick = onWorkoutClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        CompactHomeCard(
            title = "Nutrition",
            value = nutritionCardValue,
            description = nutritionCardDescription,
            icon = Icons.Filled.Restaurant,
            progress = nutritionProgress,
            buttonText = "Open Nutrition →",
            onClick = onNutritionClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProgressSummaryCard(
            weight = if (weight == "-") "-" else "$weight kg",
            bodyFat = if (latestBodyFat == "-") "-" else "$latestBodyFat%",
            onClick = onProgressClick
        )

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun CompactHomeCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    progress: Float,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProgressSummaryCard(
    weight: String,
    bodyFat: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShowChart,
                        contentDescription = "Progress",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Text(
                    text = "Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Open Progress →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    title = "Weight",
                    value = weight,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MiniStat(
                    title = "Body Fat",
                    value = bodyFat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MiniStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}