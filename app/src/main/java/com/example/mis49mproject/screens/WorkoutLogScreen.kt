package com.example.mis49mproject.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mis49mproject.firebase.syncUserDataToFirebase
import com.example.mis49mproject.firebase.syncWorkoutDataToFirebase
import com.example.mis49mproject.score.calculateAndSaveGlowScores
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DayProgramExercise(
    val dayName: String,
    val exerciseName: String,
    val weight: String,
    val sets: String,
    val reps: String,
    val completed: Boolean
)

data class WorkoutHistoryEntry(
    val date: String,
    val dayName: String,
    val exerciseName: String,
    val weight: String,
    val sets: String,
    val reps: String
)

@Composable
fun WorkoutLogScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)

    val days = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    val exerciseCategories = mapOf(
        "Chest" to listOf(
            "Bench Press",
            "Incline Bench Press",
            "Dumbbell Press",
            "Incline Dumbbell Press",
            "Chest Fly",
            "Cable Crossover",
            "Push Up"
        ),
        "Back" to listOf(
            "Lat Pulldown",
            "Cable Row",
            "Barbell Row",
            "Dumbbell Row",
            "Deadlift",
            "Pull Up",
            "Assisted Pull Up"
        ),
        "Shoulders" to listOf(
            "Shoulder Press",
            "Dumbbell Shoulder Press",
            "Lateral Raise",
            "Front Raise",
            "Rear Delt Fly",
            "Face Pull",
            "Upright Row"
        ),
        "Biceps" to listOf(
            "Biceps Curl",
            "Hammer Curl",
            "Barbell Curl",
            "Preacher Curl",
            "Cable Curl"
        ),
        "Triceps" to listOf(
            "Triceps Pushdown",
            "Overhead Triceps Extension",
            "Skull Crusher",
            "Close Grip Bench Press",
            "Dips"
        ),
        "Legs" to listOf(
            "Squat",
            "Leg Press",
            "Leg Extension",
            "Leg Curl",
            "Romanian Deadlift",
            "Hip Thrust",
            "Lunges",
            "Bulgarian Split Squat",
            "Calf Raise"
        ),
        "Core" to listOf(
            "Plank",
            "Crunch",
            "Leg Raise",
            "Russian Twist",
            "Cable Crunch"
        ),
        "Cardio" to listOf(
            "Treadmill",
            "Bike",
            "Rowing Machine",
            "Stair Climber"
        )
    )

    fun decimalOnly(value: String): String {
        val cleaned = value
            .replace(",", ".")
            .filter { it.isDigit() || it == '.' }

        if (cleaned == ".") return ""

        val firstDotIndex = cleaned.indexOf('.')

        val normalized = if (firstDotIndex == -1) {
            cleaned
        } else {
            cleaned.substring(0, firstDotIndex + 1) +
                    cleaned.substring(firstDotIndex + 1).replace(".", "")
        }

        return when {
            normalized == "." -> ""
            normalized.startsWith(".") -> "0$normalized"
            else -> normalized
        }
    }

    fun numberOnly(value: String): String {
        return value.filter { it.isDigit() }
    }

    fun currentDate(): String {
        return SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    val todayName = SimpleDateFormat(
        "EEEE",
        Locale.ENGLISH
    ).format(Date())

    var selectedDay by remember { mutableStateOf<String?>(todayName) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    var dayMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var exerciseMenuExpanded by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }

    val programExercises = remember {
        val savedData = sharedPreferences.getString("day_program_exercises_v1", "") ?: ""

        val loadedExercises = savedData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                when (parts.size) {
                    6 -> DayProgramExercise(
                        dayName = parts[0],
                        exerciseName = parts[1],
                        weight = parts[2],
                        sets = parts[3],
                        reps = parts[4],
                        completed = false
                    )

                    5 -> DayProgramExercise(
                        dayName = parts[0],
                        exerciseName = parts[1],
                        weight = parts[2],
                        sets = "",
                        reps = parts[3],
                        completed = false
                    )

                    else -> null
                }
            }

        mutableStateListOf<DayProgramExercise>().apply {
            addAll(loadedExercises)
        }
    }

    val workoutHistory = remember {
        val savedData = sharedPreferences.getString("workout_history_v1", "") ?: ""

        val loadedHistory = savedData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                when (parts.size) {
                    6 -> WorkoutHistoryEntry(
                        date = parts[0],
                        dayName = parts[1],
                        exerciseName = parts[2],
                        weight = parts[3],
                        sets = parts[4],
                        reps = parts[5]
                    )

                    5 -> WorkoutHistoryEntry(
                        date = parts[0],
                        dayName = parts[1],
                        exerciseName = parts[2],
                        weight = parts[3],
                        sets = "",
                        reps = parts[4]
                    )

                    else -> null
                }
            }

        mutableStateListOf<WorkoutHistoryEntry>().apply {
            addAll(loadedHistory)
        }
    }

    fun saveProgramExercises() {
        val data = programExercises.joinToString("\n") { exercise ->
            "${exercise.dayName}|${exercise.exerciseName}|${exercise.weight}|${exercise.sets}|${exercise.reps}|false"
        }

        sharedPreferences.edit()
            .putString("day_program_exercises_v1", data)
            .apply()

        syncWorkoutDataToFirebase(context)
        calculateAndSaveGlowScores(context)
        syncUserDataToFirebase(context)
    }

    fun saveWorkoutHistory() {
        val data = workoutHistory.joinToString("\n") { entry ->
            "${entry.date}|${entry.dayName}|${entry.exerciseName}|${entry.weight}|${entry.sets}|${entry.reps}"
        }

        sharedPreferences.edit()
            .putString("workout_history_v1", data)
            .apply()

        syncWorkoutDataToFirebase(context)
        calculateAndSaveGlowScores(context)
        syncUserDataToFirebase(context)
    }

    fun isExerciseCompletedToday(exercise: DayProgramExercise): Boolean {
        val today = currentDate()

        return workoutHistory.any { entry ->
            entry.date == today &&
                    entry.dayName == exercise.dayName &&
                    entry.exerciseName == exercise.exerciseName
        }
    }

    val selectedDayExercises = programExercises.filter { it.dayName == selectedDay }
    val completedCount = selectedDayExercises.count { isExerciseCompletedToday(it) }
    val totalCount = selectedDayExercises.size

    val completionProgress = if (totalCount > 0) {
        completedCount.toFloat() / totalCount.toFloat()
    } else {
        0f
    }

    val todayExercises = programExercises.filter { it.dayName == todayName }
    val todayCompletedCount = todayExercises.count { isExerciseCompletedToday(it) }
    val todayTotalCount = todayExercises.size

    val todayWorkoutScore = if (todayTotalCount > 0) {
        ((todayCompletedCount.toDouble() / todayTotalCount.toDouble()) * 40.0)
            .roundToInt()
            .coerceIn(0, 40)
    } else {
        0
    }

    val todayWorkoutProgress = if (todayTotalCount > 0) {
        todayCompletedCount.toFloat() / todayTotalCount.toFloat()
    } else {
        0f
    }

    val todayWorkoutMessage = when {
        todayTotalCount == 0 -> "No workout planned today"
        todayCompletedCount == todayTotalCount -> "Workout completed"
        todayCompletedCount > 0 -> "$todayCompletedCount / $todayTotalCount exercises completed"
        else -> "Start today's workout"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Workout",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Build your plan, complete exercises and log performance.",
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
                            text = "Today's Workout Score",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$todayWorkoutScore / 40",
                            fontSize = 36.sp,
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
                            contentDescription = "Workout Score",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = todayWorkoutMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(9.dp))

                LinearProgressIndicator(
                    progress = { todayWorkoutProgress.coerceIn(0f, 1f) },
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
                SectionHeaderWorkout(
                    icon = Icons.Filled.Today,
                    title = "Select Day"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box {
                    Button(
                        onClick = { dayMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = selectedDay ?: "Choose Day",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = dayMenuExpanded,
                        onDismissRequest = { dayMenuExpanded = false }
                    ) {
                        days.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = day
                                    selectedCategory = null
                                    selectedExercise = null
                                    dayMenuExpanded = false
                                    message = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        selectedDay == null -> "Select a day to unlock exercise selection."
                        totalCount > 0 -> "$completedCount / $totalCount exercises completed"
                        else -> "No exercises added for this day yet."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedDay != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { completionProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedDay == null) {
            EmptyWorkoutState(
                text = "Choose a day above to create or edit a workout plan."
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SectionHeaderWorkout(
                        icon = Icons.Filled.Add,
                        title = "Add Exercise"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Adding to: $selectedDay",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box {
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = selectedCategory ?: "Choose Category",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            exerciseCategories.keys.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        selectedExercise = null
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val availableExercises = selectedCategory
                        ?.let { exerciseCategories[it] }
                        ?.filter { exerciseName ->
                            programExercises.none {
                                it.dayName == selectedDay && it.exerciseName == exerciseName
                            }
                        } ?: emptyList()

                    Box {
                        OutlinedButton(
                            onClick = { exerciseMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            enabled = selectedCategory != null && availableExercises.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = when {
                                    selectedCategory == null -> "Choose Exercise"
                                    availableExercises.isEmpty() -> "All exercises added"
                                    else -> selectedExercise ?: "Choose Exercise"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = exerciseMenuExpanded,
                            onDismissRequest = { exerciseMenuExpanded = false }
                        ) {
                            availableExercises.forEach { exercise ->
                                DropdownMenuItem(
                                    text = { Text(exercise) },
                                    onClick = {
                                        selectedExercise = exercise
                                        exerciseMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCategory != null && availableExercises.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "All exercises in this category are already added for $selectedDay.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (selectedExercise != null) {
                                val alreadyExists = programExercises.any { exercise ->
                                    exercise.dayName == selectedDay &&
                                            exercise.exerciseName == selectedExercise
                                }

                                if (alreadyExists) {
                                    message = "This exercise is already added for $selectedDay."
                                } else {
                                    programExercises.add(
                                        DayProgramExercise(
                                            dayName = selectedDay!!,
                                            exerciseName = selectedExercise!!,
                                            weight = "",
                                            sets = "",
                                            reps = "",
                                            completed = false
                                        )
                                    )

                                    saveProgramExercises()

                                    selectedCategory = null
                                    selectedExercise = null
                                    message = "Exercise added to $selectedDay."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        enabled = selectedExercise != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Add Exercise",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$selectedDay Program",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedDayExercises.isEmpty()) {
                EmptyWorkoutState("No exercises for $selectedDay yet.")
            } else {
                programExercises.forEachIndexed { index, exercise ->
                    if (exercise.dayName == selectedDay) {
                        val isCompletedToday = isExerciseCompletedToday(exercise)

                        WorkoutExerciseCard(
                            exercise = exercise.copy(completed = isCompletedToday),
                            onCheckedChange = { checked ->
                                val todayNameNow = SimpleDateFormat(
                                    "EEEE",
                                    Locale.ENGLISH
                                ).format(Date())

                                val todayDate = currentDate()

                                if (exercise.dayName != todayNameNow) {
                                    message = "You can only complete today's workout."
                                    return@WorkoutExerciseCard
                                }

                                if (checked) {
                                    if (
                                        exercise.weight.isBlank() ||
                                        exercise.sets.isBlank() ||
                                        exercise.reps.isBlank()
                                    ) {
                                        message = "Enter kg, sets and reps before completing this exercise."
                                    } else {
                                        val existingLogIndex = workoutHistory.indexOfFirst { entry ->
                                            entry.date == todayDate &&
                                                    entry.dayName == exercise.dayName &&
                                                    entry.exerciseName == exercise.exerciseName
                                        }

                                        val updatedEntry = WorkoutHistoryEntry(
                                            date = todayDate,
                                            dayName = exercise.dayName,
                                            exerciseName = exercise.exerciseName,
                                            weight = exercise.weight,
                                            sets = exercise.sets,
                                            reps = exercise.reps
                                        )

                                        if (existingLogIndex >= 0) {
                                            workoutHistory[existingLogIndex] = updatedEntry
                                            message = "Workout log updated for today."
                                        } else {
                                            workoutHistory.add(0, updatedEntry)
                                            message = "Workout logged successfully."
                                        }

                                        saveWorkoutHistory()
                                    }
                                } else {
                                    workoutHistory.removeAll { entry ->
                                        entry.date == todayDate &&
                                                entry.dayName == exercise.dayName &&
                                                entry.exerciseName == exercise.exerciseName
                                    }

                                    saveWorkoutHistory()
                                    message = "Exercise marked as pending."
                                }
                            },
                            onWeightChange = { newWeight ->
                                programExercises[index] = exercise.copy(
                                    weight = decimalOnly(newWeight),
                                    completed = false
                                )
                                saveProgramExercises()
                            },
                            onSetsChange = { newSets ->
                                programExercises[index] = exercise.copy(
                                    sets = numberOnly(newSets),
                                    completed = false
                                )
                                saveProgramExercises()
                            },
                            onRepsChange = { newReps ->
                                programExercises[index] = exercise.copy(
                                    reps = numberOnly(newReps),
                                    completed = false
                                )
                                saveProgramExercises()
                            },
                            onRemoveClick = {
                                programExercises.removeAt(index)

                                saveProgramExercises()

                                message = "Exercise removed from $selectedDay."
                            }
                        )
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeaderWorkout(
            icon = Icons.Filled.History,
            title = "Workout History"
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (workoutHistory.isEmpty()) {
            EmptyWorkoutState("No completed workout logs yet.")
        } else {
            workoutHistory.forEachIndexed { index, entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = entry.exerciseName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (entry.sets.isBlank()) {
                                "${entry.weight} kg x ${entry.reps} reps"
                            } else {
                                "${entry.weight} kg • ${entry.sets} sets x ${entry.reps} reps"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${entry.dayName} • ${entry.date}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(9.dp))

                        OutlinedButton(
                            onClick = {
                                workoutHistory.removeAt(index)
                                saveWorkoutHistory()
                                message = "Workout log deleted."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Log",
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Delete Log",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun WorkoutExerciseCard(
    exercise: DayProgramExercise,
    onCheckedChange: (Boolean) -> Unit,
    onWeightChange: (String) -> Unit,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (exercise.completed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Icon(
                        imageVector = if (exercise.completed) Icons.Filled.CheckCircle else Icons.Filled.Pending,
                        contentDescription = if (exercise.completed) "Completed" else "Pending",
                        tint = if (exercise.completed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                        text = if (exercise.completed) "Completed" else "Pending",
                        fontSize = 12.sp,
                        color = if (exercise.completed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Checkbox(
                    checked = exercise.completed,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = exercise.weight,
                onValueChange = onWeightChange,
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = exercise.sets,
                    onValueChange = onSetsChange,
                    label = { Text("sets") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = exercise.reps,
                    onValueChange = onRepsChange,
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            OutlinedButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove Exercise",
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Remove Exercise",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SectionHeaderWorkout(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
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

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyWorkoutState(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(11.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}