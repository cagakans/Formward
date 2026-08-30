package com.formward.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formward.app.firebase.syncUserDataToFirebase
import com.formward.app.score.calculateAndSaveFormScores
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckInScreen() {
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val workoutPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())

    val savedWorkoutData = workoutPreferences.getString("day_program_exercises_v1", "") ?: ""

    val hasWorkoutPlanToday = savedWorkoutData
        .lines()
        .filter { it.isNotBlank() }
        .any { line ->
            val parts = line.split("|")
            parts.size >= 5 && parts[0] == todayName
        }

    val savedMissionDate = sharedPreferences.getString("mission_date", "") ?: ""
    val isTodayMission = savedMissionDate == todayDate

    var workoutCompleted by remember {
        mutableStateOf(
            if (isTodayMission && hasWorkoutPlanToday) {
                sharedPreferences.getBoolean("mission_workout", false)
            } else {
                false
            }
        )
    }

    var recoveryDay by remember {
        mutableStateOf(
            if (isTodayMission && !hasWorkoutPlanToday) {
                sharedPreferences.getBoolean("mission_recovery", false)
            } else {
                false
            }
        )
    }

    var nutritionCompleted by remember {
        mutableStateOf(
            if (isTodayMission) {
                sharedPreferences.getBoolean("mission_nutrition", false)
            } else {
                false
            }
        )
    }

    var waterCompleted by remember {
        mutableStateOf(
            if (isTodayMission) {
                sharedPreferences.getBoolean("mission_water", false)
            } else {
                false
            }
        )
    }

    var movementCompleted by remember {
        mutableStateOf(
            if (isTodayMission) {
                sharedPreferences.getBoolean(
                    "mission_movement",
                    sharedPreferences.getBoolean("mission_steps", false)
                )
            } else {
                false
            }
        )
    }

    var sleepCompleted by remember {
        mutableStateOf(
            if (isTodayMission) {
                sharedPreferences.getBoolean("mission_sleep", false)
            } else {
                false
            }
        )
    }

    var savedMessage by remember { mutableStateOf("") }

    val workoutOrRecoveryScore = when {
        hasWorkoutPlanToday && workoutCompleted -> 25
        !hasWorkoutPlanToday && recoveryDay -> 25
        else -> 0
    }

    val missionScore =
        workoutOrRecoveryScore +
                (if (nutritionCompleted) 25 else 0) +
                (if (waterCompleted) 15 else 0) +
                (if (movementCompleted) 15 else 0) +
                (if (sleepCompleted) 20 else 0)


    val missionMessage = when {
        missionScore >= 85 -> "Elite mission day"
        missionScore >= 60 -> "Strong mission progress"
        missionScore > 0 -> "Good start"
        else -> "Start your first mission item"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Daily Mission",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Complete today’s discipline tasks.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mission Completion",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$missionScore",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Mission",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$missionMessage • $missionScore / 100",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(9.dp))

                LinearProgressIndicator(
                    progress = { missionScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mission Checklist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Check off what you completed today.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                MissionItem(
                    title = "Complete Workout Plan",
                    description = if (hasWorkoutPlanToday) {
                        "Finish today’s workout program."
                    } else {
                        "No workout planned today. Use Recovery Day."
                    },
                    icon = Icons.Filled.FitnessCenter,
                    checked = workoutCompleted,
                    enabled = hasWorkoutPlanToday,
                    onCheckedChange = {
                        workoutCompleted = it
                        if (it) recoveryDay = false
                    }
                )

                MissionItem(
                    title = "Recovery Day",
                    description = if (hasWorkoutPlanToday) {
                        "Workout planned today, so recovery is locked."
                    } else {
                        "Use this for an intentional rest day."
                    },
                    icon = Icons.Filled.CheckCircle,
                    checked = recoveryDay,
                    enabled = !hasWorkoutPlanToday,
                    onCheckedChange = {
                        recoveryDay = it
                        if (it) workoutCompleted = false
                    }
                )

                MissionItem(
                    title = "Hit Nutrition Target",
                    description = "Stay close to calorie and protein goals.",
                    icon = Icons.Filled.Restaurant,
                    checked = nutritionCompleted,
                    onCheckedChange = { nutritionCompleted = it }
                )

                MissionItem(
                    title = "Reach Water Goal",
                    description = "Complete your daily water intake.",
                    icon = Icons.Filled.LocalDrink,
                    checked = waterCompleted,
                    onCheckedChange = { waterCompleted = it }
                )

                MissionItem(
                    title = "Daily Movement",
                    description = "Walk, move, or stay active.",
                    icon = Icons.Filled.DirectionsWalk,
                    checked = movementCompleted,
                    onCheckedChange = { movementCompleted = it }
                )

                MissionItem(
                    title = "Sleep 7+ Hours",
                    description = "Recover properly for tomorrow.",
                    icon = Icons.Filled.Nightlight,
                    checked = sleepCompleted,
                    onCheckedChange = { sleepCompleted = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val finalWorkoutCompleted = if (hasWorkoutPlanToday) workoutCompleted else false
                val finalRecoveryDay = if (!hasWorkoutPlanToday) recoveryDay else false

                sharedPreferences.edit()
                    .putString("mission_date", todayDate)
                    .putBoolean("mission_workout", finalWorkoutCompleted)
                    .putBoolean("mission_recovery", finalRecoveryDay)
                    .putBoolean("mission_nutrition", nutritionCompleted)
                    .putBoolean("mission_water", waterCompleted)
                    .putBoolean("mission_movement", movementCompleted)
                    .putBoolean("mission_steps", movementCompleted)
                    .putBoolean("mission_sleep", sleepCompleted)
                    .putInt("mission_score", missionScore)
                    .apply()

                val result = calculateAndSaveFormScores(context)
                syncUserDataToFirebase(context)

                savedMessage =
                    "Mission saved. Mission: $missionScore/100. Today’s Form Score: ${result.dailyScore}/100."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Save Daily Mission",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (savedMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = savedMessage,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun MissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = when {
            checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            else -> MaterialTheme.colorScheme.background.copy(alpha = 0.45f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = if (checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = when {
                        checked -> MaterialTheme.colorScheme.primary
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    }
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}