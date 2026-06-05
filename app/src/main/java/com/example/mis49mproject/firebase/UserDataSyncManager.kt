package com.example.mis49mproject.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncUserDataToFirebase(
    context: Context,
    onComplete: ((String) -> Unit)? = null
) {
    val glowPreferences = context.getSharedPreferences("glowup_data", Context.MODE_PRIVATE)
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)
    val photoPreferences = context.getSharedPreferences("photo_checkin_data", Context.MODE_PRIVATE)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun parseDailyScores(dailyScoresData: String): List<Map<String, Any>> {
        return dailyScoresData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                if (parts.size == 2) {
                    val score = parts[1].toIntOrNull()

                    if (score != null) {
                        mapOf(
                            "date" to parts[0],
                            "score" to score.coerceIn(0, 100)
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
    }

    fun parsePhotoEntries(photoEntriesData: String): List<Map<String, Any>> {
        return photoEntriesData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                if (parts.size == 2) {
                    mapOf(
                        "path" to parts[0],
                        "date" to parts[1]
                    )
                } else {
                    null
                }
            }
    }

    fun syncCurrentUser() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onComplete?.invoke("Firebase user not ready.")
            return
        }

        val todayDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())

        val dailyScoresData = glowPreferences.getString("daily_scores_v1", "") ?: ""
        val dailyScoresList = parseDailyScores(dailyScoresData)

        val photoEntriesData = photoPreferences.getString("photo_entries", "") ?: ""
        val photoEntriesList = parsePhotoEntries(photoEntriesData)

        val photoEntryCount = photoEntriesList.size

        val latestPhotoDate = photoEntriesList
            .firstOrNull()
            ?.get("date")
            ?.toString()
            ?: ""

        val missionMovement = glowPreferences.getBoolean(
            "mission_movement",
            glowPreferences.getBoolean("mission_steps", false)
        )

        val userCloudData = hashMapOf<String, Any>(
            // Profile basics
            "gender" to (glowPreferences.getString("gender", "") ?: ""),
            "height" to (glowPreferences.getString("height", "") ?: ""),
            "weight" to (glowPreferences.getString("weight", "") ?: ""),
            "goal" to (glowPreferences.getString("goal", "") ?: ""),
            "latestBodyFat" to (glowPreferences.getString("latest_body_fat", "-") ?: "-"),

            // Nutrition profile details
            "age" to (nutritionPreferences.getString("age", "") ?: ""),
            "activityLevel" to (nutritionPreferences.getString("activity_level", "") ?: ""),
            "nutritionDate" to (nutritionPreferences.getString("nutrition_date", "") ?: ""),

            // Scores
            "todayDate" to todayDate,
            "todayGlowScore" to glowPreferences.getInt("today_glow_score", 0),
            "weeklyGlowScore" to glowPreferences.getInt("weekly_glow_score", 0),
            "streakCount" to glowPreferences.getInt("streak_count", 0),
            "lastScore" to glowPreferences.getInt("last_score", 0),

            // Keep raw score data for compatibility
            "dailyScoresData" to dailyScoresData,

            // Structured score data for easier Firebase reading
            "dailyScores" to dailyScoresList,

            // Mission
            "missionDate" to (glowPreferences.getString("mission_date", "") ?: ""),
            "missionWorkout" to glowPreferences.getBoolean("mission_workout", false),
            "missionRecovery" to glowPreferences.getBoolean("mission_recovery", false),
            "missionNutrition" to glowPreferences.getBoolean("mission_nutrition", false),
            "missionWater" to glowPreferences.getBoolean("mission_water", false),
            "missionMovement" to missionMovement,

            // Keep old key for compatibility
            "missionSteps" to missionMovement,

            "missionSleep" to glowPreferences.getBoolean("mission_sleep", false),
            "missionScore" to glowPreferences.getInt("mission_score", 0),

            // Setup
            "initialSetupCompleted" to glowPreferences.getBoolean("initial_setup_completed", false),

            // Photo check-in
            "photoEntryCount" to photoEntryCount,
            "latestPhotoDate" to latestPhotoDate,

            // Keep raw photo metadata for compatibility
            "photoEntriesData" to photoEntriesData,

            // Structured photo metadata
            "photoEntries" to photoEntriesList,

            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("privateData")
            .document("profile")
            .set(userCloudData, SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke("User data synced.")
            }
            .addOnFailureListener { error ->
                onComplete?.invoke("User data sync failed: ${error.message}")
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