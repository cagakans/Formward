package com.example.mis49mproject.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncProgressDataToFirebase(
    context: Context,
    onComplete: ((String) -> Unit)? = null
) {
    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun parseProgressHistory(progressHistoryData: String): List<Map<String, Any>> {
        return progressHistoryData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")

                if (parts.size == 14) {
                    mapOf(
                        "date" to parts[0],
                        "gender" to parts[1],
                        "bodyFat" to parts[2],
                        "height" to parts[3],
                        "weight" to parts[4],
                        "neck" to parts[5],
                        "waist" to parts[6],
                        "hip" to parts[7],
                        "chest" to parts[8],
                        "shoulders" to parts[9],
                        "arm" to parts[10],
                        "forearm" to parts[11],
                        "thigh" to parts[12],
                        "calf" to parts[13]
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

        val progressHistoryData = formPreferences.getString("progress_history_v1", "") ?: ""
        val progressHistoryList = parseProgressHistory(progressHistoryData)

        val latestBodyFat = formPreferences.getString("latest_body_fat", "-") ?: "-"

        val progressCloudData = hashMapOf<String, Any>(
            "gender" to (formPreferences.getString("gender", "") ?: ""),

            "height" to (formPreferences.getString("height", "") ?: ""),
            "weight" to (formPreferences.getString("weight", "") ?: ""),
            "neck" to (formPreferences.getString("neck", "") ?: ""),
            "waist" to (formPreferences.getString("waist", "") ?: ""),
            "hip" to (formPreferences.getString("hip", "") ?: ""),

            "chest" to (formPreferences.getString("chest", "") ?: ""),
            "shoulders" to (formPreferences.getString("shoulders", "") ?: ""),
            "arm" to (formPreferences.getString("arm", "") ?: ""),
            "forearm" to (formPreferences.getString("forearm", "") ?: ""),
            "thigh" to (formPreferences.getString("thigh", "") ?: ""),
            "calf" to (formPreferences.getString("calf", "") ?: ""),

            "latestBodyFat" to latestBodyFat,

            // Keep raw data for compatibility with your local format
            "progressHistoryData" to progressHistoryData,

            // Structured version for easier Firebase reading
            "progressHistory" to progressHistoryList,

            "progressLogCount" to progressHistoryList.size,
            "hasProgressHistory" to progressHistoryList.isNotEmpty(),
            "todayDate" to todayDate,

            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("privateData")
            .document("progress")
            .set(progressCloudData, SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke("Progress data synced.")
            }
            .addOnFailureListener { error ->
                onComplete?.invoke("Progress sync failed: ${error.message}")
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