package com.formward.app.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncNutritionDataToFirebase(
    context: Context,
    onComplete: ((String) -> Unit)? = null
) {
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

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

        val savedNutritionDate = nutritionPreferences.getString("nutrition_date", "") ?: ""
        val isTodayNutrition = savedNutritionDate == todayDate

        val calorieConsumed = if (isTodayNutrition) {
            nutritionPreferences.getString("calorie_consumed", "") ?: ""
        } else {
            ""
        }

        val proteinConsumed = if (isTodayNutrition) {
            nutritionPreferences.getString("protein_consumed", "") ?: ""
        } else {
            ""
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

        val waterConsumed = if (isTodayNutrition) {
            nutritionPreferences.getString("water_consumed", "") ?: ""
        } else {
            ""
        }

        val nutritionScore = if (isTodayNutrition) {
            nutritionPreferences.getInt("nutrition_score", 0)
        } else {
            0
        }

        val nutritionCloudData = hashMapOf<String, Any>(
            "age" to (nutritionPreferences.getString("age", "") ?: ""),
            "activityLevel" to (nutritionPreferences.getString("activity_level", "") ?: ""),

            "todayDate" to todayDate,
            "nutritionDate" to savedNutritionDate,
            "isTodayNutrition" to isTodayNutrition,

            "calorieTarget" to (nutritionPreferences.getString("calorie_target", "") ?: ""),
            "calorieConsumed" to calorieConsumed,

            "proteinTarget" to (nutritionPreferences.getString("protein_target", "") ?: ""),
            "proteinConsumed" to proteinConsumed,

            "carbsTarget" to (nutritionPreferences.getString("carbs_target", "") ?: ""),
            "carbsConsumed" to carbsConsumed,

            "fatTarget" to (nutritionPreferences.getString("fat_target", "") ?: ""),
            "fatConsumed" to fatConsumed,

            "waterTarget" to (nutritionPreferences.getString("water_target", "") ?: ""),
            "waterConsumed" to waterConsumed,

            "nutritionScore" to nutritionScore,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("privateData")
            .document("nutrition")
            .set(nutritionCloudData, SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke("Nutrition data synced.")
            }
            .addOnFailureListener { error ->
                onComplete?.invoke("Nutrition sync failed: ${error.message}")
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