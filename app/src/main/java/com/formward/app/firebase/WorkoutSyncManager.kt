package com.formward.app.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncWorkoutDataToFirebase(
    context: Context,
    onComplete: ((String) -> Unit)? = null
) {
    val workoutPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun parseProgramExercises(programData: String): List<Map<String, Any>> {
        return programData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                when {
                    parts.size >= 6 -> {
                        mapOf(
                            "dayName" to parts[0],
                            "exerciseName" to parts[1],
                            "weight" to parts[2],
                            "sets" to parts[3],
                            "reps" to parts[4],
                            "completed" to (parts[5].toBooleanStrictOrNull() ?: false)
                        )
                    }

                    parts.size == 5 -> {
                        mapOf(
                            "dayName" to parts[0],
                            "exerciseName" to parts[1],
                            "weight" to parts[2],
                            "sets" to "",
                            "reps" to parts[3],
                            "completed" to false
                        )
                    }

                    else -> null
                }
            }
    }

    fun parseWorkoutHistory(historyData: String): List<Map<String, Any>> {
        return historyData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                when {
                    parts.size >= 6 -> {
                        mapOf(
                            "date" to parts[0],
                            "dayName" to parts[1],
                            "exerciseName" to parts[2],
                            "weight" to parts[3],
                            "sets" to parts[4],
                            "reps" to parts[5]
                        )
                    }

                    parts.size == 5 -> {
                        mapOf(
                            "date" to parts[0],
                            "dayName" to parts[1],
                            "exerciseName" to parts[2],
                            "weight" to parts[3],
                            "sets" to "",
                            "reps" to parts[4]
                        )
                    }

                    else -> null
                }
            }
    }

    fun syncCurrentUser() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onComplete?.invoke("Firebase user not ready.")
            return
        }

        val programData = workoutPreferences.getString("day_program_exercises_v1", "") ?: ""
        val historyData = workoutPreferences.getString("workout_history_v1", "") ?: ""

        val programExercises = parseProgramExercises(programData)
        val workoutHistory = parseWorkoutHistory(historyData)

        val todayDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())

        val todayDisplay = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())

        val todayName = SimpleDateFormat(
            "EEEE",
            Locale.ENGLISH
        ).format(Date())

        val todayProgramExercises = programExercises.filter { exercise ->
            exercise["dayName"] == todayName
        }

        val todayCompletedHistory = workoutHistory.filter { entry ->
            entry["date"] == todayDisplay &&
                    entry["dayName"] == todayName
        }

        val todayCompletedExerciseNames = todayCompletedHistory
            .mapNotNull { it["exerciseName"]?.toString() }
            .toSet()

        val todayCompletedCount = todayProgramExercises.count { exercise ->
            todayCompletedExerciseNames.contains(exercise["exerciseName"]?.toString())
        }

        val todayProgramCount = todayProgramExercises.size

        val todayCompletionPercent = if (todayProgramCount > 0) {
            ((todayCompletedCount.toDouble() / todayProgramCount.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }

        val workoutCloudData = hashMapOf<String, Any>(
            // Raw local data for compatibility
            "programData" to programData,
            "historyData" to historyData,

            // Structured data for easier Firebase reading
            "programExercises" to programExercises,
            "workoutHistory" to workoutHistory,

            // Counts
            "programExerciseCount" to programExercises.size,
            "workoutHistoryCount" to workoutHistory.size,

            // Today summary
            "todayDate" to todayDate,
            "todayDisplay" to todayDisplay,
            "todayName" to todayName,
            "hasWorkoutPlanToday" to todayProgramExercises.isNotEmpty(),
            "todayProgramCount" to todayProgramCount,
            "todayCompletedCount" to todayCompletedCount,
            "todayCompletionPercent" to todayCompletionPercent,
            "todayProgramExercises" to todayProgramExercises,
            "todayCompletedHistory" to todayCompletedHistory,

            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("privateData")
            .document("workout")
            .set(workoutCloudData, SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke("Workout data synced.")
            }
            .addOnFailureListener { error ->
                onComplete?.invoke("Workout sync failed: ${error.message}")
            }
    }

    if (auth.currentUser != null) {
        syncCurrentUser()
    } else {
        auth.signInAnonymously()
            .addOnSuccessListener {
                syncCurrentUser()
            }
            .addOnFailureListener { error ->
                onComplete?.invoke("Firebase sign-in failed: ${error.message}")
            }
    }
}