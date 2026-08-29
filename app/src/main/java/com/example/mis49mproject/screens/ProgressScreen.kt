package com.example.mis49mproject.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mis49mproject.firebase.syncProgressDataToFirebase
import com.example.mis49mproject.firebase.syncUserDataToFirebase
import com.example.mis49mproject.score.calculateAndSaveFormScores
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10

data class ProgressEntry(
    val date: String,
    val gender: String,
    val bodyFat: String,
    val height: String,
    val weight: String,
    val neck: String,
    val waist: String,
    val hip: String,
    val chest: String,
    val shoulders: String,
    val arm: String,
    val forearm: String,
    val thigh: String,
    val calf: String
)

@Composable
fun ProgressScreen(
    onPhotoCheckInClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val photoPreferences = context.getSharedPreferences("photo_checkin_data", Context.MODE_PRIVATE)

    val selectedGender = sharedPreferences.getString("gender", null)

    val todayDateOnly = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).format(Date())

    val progressPhotos = remember {
        val savedData = photoPreferences.getString("photo_entries", "") ?: ""

        savedData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
    }

    var height by remember { mutableStateOf(sharedPreferences.getString("height", "") ?: "") }
    var weight by remember { mutableStateOf(sharedPreferences.getString("weight", "") ?: "") }
    var neck by remember { mutableStateOf(sharedPreferences.getString("neck", "") ?: "") }
    var waist by remember { mutableStateOf(sharedPreferences.getString("waist", "") ?: "") }
    var hip by remember { mutableStateOf(sharedPreferences.getString("hip", "") ?: "") }

    var chest by remember { mutableStateOf(sharedPreferences.getString("chest", "") ?: "") }
    var shoulders by remember { mutableStateOf(sharedPreferences.getString("shoulders", "") ?: "") }
    var arm by remember { mutableStateOf(sharedPreferences.getString("arm", "") ?: "") }
    var forearm by remember { mutableStateOf(sharedPreferences.getString("forearm", "") ?: "") }
    var thigh by remember { mutableStateOf(sharedPreferences.getString("thigh", "") ?: "") }
    var calf by remember { mutableStateOf(sharedPreferences.getString("calf", "") ?: "") }

    var bodyFatSavedMessage by remember { mutableStateOf("") }
    var measurementsSavedMessage by remember { mutableStateOf("") }
    var historyMessage by remember { mutableStateOf("") }

    val progressEntries = remember {
        val savedData = sharedPreferences.getString("progress_history_v1", "") ?: ""

        val loadedEntries = savedData
            .lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size == 14) {
                    ProgressEntry(
                        date = parts[0],
                        gender = parts[1],
                        bodyFat = parts[2],
                        height = parts[3],
                        weight = parts[4],
                        neck = parts[5],
                        waist = parts[6],
                        hip = parts[7],
                        chest = parts[8],
                        shoulders = parts[9],
                        arm = parts[10],
                        forearm = parts[11],
                        thigh = parts[12],
                        calf = parts[13]
                    )
                } else {
                    null
                }
            }

        mutableStateListOf<ProgressEntry>().apply {
            addAll(loadedEntries)
        }
    }

    fun hasBodyFatValue(entry: ProgressEntry): Boolean {
        return entry.bodyFat.isNotBlank() && entry.bodyFat != "-"
    }

    fun findTodayIndex(): Int {
        return progressEntries.indexOfFirst { entry ->
            entry.date.startsWith(todayDateOnly)
        }
    }

    fun saveProgressHistory() {
        val data = progressEntries.joinToString("\n") { entry ->
            "${entry.date}|${entry.gender}|${entry.bodyFat}|${entry.height}|${entry.weight}|${entry.neck}|${entry.waist}|${entry.hip}|${entry.chest}|${entry.shoulders}|${entry.arm}|${entry.forearm}|${entry.thigh}|${entry.calf}"
        }

        sharedPreferences.edit()
            .putString("progress_history_v1", data)
            .apply()
    }

    fun updateLatestProgressValuesAfterDelete() {
        val latestEntry = progressEntries.firstOrNull()
        val latestBodyFatEntry = progressEntries.firstOrNull { entry ->
            hasBodyFatValue(entry)
        }

        val editor = sharedPreferences.edit()

        editor.putString("latest_body_fat", latestBodyFatEntry?.bodyFat ?: "-")

        if (latestEntry != null) {
            editor
                .putString("height", latestEntry.height)
                .putString("weight", latestEntry.weight)
                .putString("neck", latestEntry.neck)
                .putString("waist", latestEntry.waist)
                .putString("hip", latestEntry.hip)
                .putString("chest", latestEntry.chest)
                .putString("shoulders", latestEntry.shoulders)
                .putString("arm", latestEntry.arm)
                .putString("forearm", latestEntry.forearm)
                .putString("thigh", latestEntry.thigh)
                .putString("calf", latestEntry.calf)
        }

        editor.apply()
    }

    fun toDouble(value: String): Double? {
        return value
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()
    }

    fun isBetween(value: Double, min: Double, max: Double): Boolean {
        return value in min..max
    }

    fun validationMessage(): String {
        val gender = selectedGender
        val heightValue = toDouble(height)
        val neckValue = toDouble(neck)
        val waistValue = toDouble(waist)
        val hipValue = toDouble(hip)

        if (gender == null) return "Set gender from Profile first."

        if (heightValue == null) return "Height is required."
        if (neckValue == null) return "Neck is required."
        if (waistValue == null) return "Waist is required."

        if (!isBetween(heightValue, 100.0, 250.0)) return "Height should be between 100 and 250 cm."
        if (!isBetween(neckValue, 20.0, 70.0)) return "Neck should be between 20 and 70 cm."
        if (!isBetween(waistValue, 40.0, 200.0)) return "Waist should be between 40 and 200 cm."

        if (waistValue <= neckValue) return "Waist must be larger than neck."

        if (gender == "Female") {
            if (hipValue == null) return "Hip is required for Female formula."
            if (!isBetween(hipValue, 40.0, 200.0)) return "Hip should be between 40 and 200 cm."
            if (waistValue + hipValue <= neckValue) return "Waist + hip must be larger than neck."
        }

        return ""
    }

    fun calculateBodyFat(): Double? {
        val gender = selectedGender ?: return null

        val heightValue = toDouble(height) ?: return null
        val neckValue = toDouble(neck) ?: return null
        val waistValue = toDouble(waist) ?: return null
        val hipValue = toDouble(hip)

        return if (gender == "Male") {
            val waistMinusNeck = waistValue - neckValue

            if (waistMinusNeck <= 0) {
                null
            } else {
                val bodyDensity =
                    1.0324 -
                            0.19077 * log10(waistMinusNeck) +
                            0.15456 * log10(heightValue)

                495 / bodyDensity - 450
            }
        } else {
            if (hipValue == null) {
                null
            } else {
                val waistPlusHipMinusNeck = waistValue + hipValue - neckValue

                if (waistPlusHipMinusNeck <= 0) {
                    null
                } else {
                    val bodyDensity =
                        1.29579 -
                                0.35004 * log10(waistPlusHipMinusNeck) +
                                0.22100 * log10(heightValue)

                    495 / bodyDensity - 450
                }
            }
        }
    }

    val errorMessage = validationMessage()
    val rawBodyFat = if (errorMessage.isBlank()) calculateBodyFat() else null
    val estimatedBodyFat =
        if (rawBodyFat != null && rawBodyFat in 2.0..75.0) rawBodyFat else null

    val hasBodyFatLogToday = progressEntries.any { entry ->
        entry.date.startsWith(todayDateOnly) && hasBodyFatValue(entry)
    }

    val bodyFatDisplay = when {
        selectedGender == null -> "--"
        errorMessage.isNotBlank() -> "--"
        estimatedBodyFat == null -> "--"
        else -> String.format(Locale.US, "%.1f%%", estimatedBodyFat)
    }

    val estimateStatus = when {
        selectedGender == null -> "Set gender from Profile first."
        errorMessage.isNotBlank() -> errorMessage
        estimatedBodyFat == null -> "Check your measurements. Result is outside realistic range."
        hasBodyFatLogToday -> "Today's body fat log exists. Saving will update it."
        else -> "Preview estimate. Save to create today's body fat log."
    }

    val canSaveBodyFat =
        estimatedBodyFat != null &&
                errorMessage.isBlank()

    fun createOrUpdateTodayEntry(
        bodyFatValue: String,
        updateBodyFat: Boolean
    ) {
        val currentDate = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        val todayIndex = findTodayIndex()
        val existingEntry = if (todayIndex >= 0) progressEntries[todayIndex] else null

        val finalBodyFat = if (updateBodyFat) {
            bodyFatValue
        } else {
            existingEntry?.bodyFat ?: "-"
        }

        val entry = ProgressEntry(
            date = currentDate,
            gender = selectedGender ?: existingEntry?.gender ?: "-",
            bodyFat = finalBodyFat,
            height = height,
            weight = weight,
            neck = neck,
            waist = waist,
            hip = hip,
            chest = chest,
            shoulders = shoulders,
            arm = arm,
            forearm = forearm,
            thigh = thigh,
            calf = calf
        )

        if (todayIndex >= 0) {
            progressEntries[todayIndex] = entry
        } else {
            progressEntries.add(0, entry)
        }

        saveProgressHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Progress Tracker",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Track body fat, measurements and photos.",
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
                            text = "Estimated Body Fat",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = bodyFatDisplay,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = "Estimated Body Fat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = estimateStatus,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (selectedGender == "Female") {
                        "Required: height, neck, waist and hip."
                    } else {
                        "Required: height, neck and waist."
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MeasurementField(
                        value = height,
                        onValueChange = {
                            height = it
                            bodyFatSavedMessage = ""
                        },
                        label = "Height",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    MeasurementField(
                        value = neck,
                        onValueChange = {
                            neck = it
                            bodyFatSavedMessage = ""
                        },
                        label = "Neck",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MeasurementField(
                        value = waist,
                        onValueChange = {
                            waist = it
                            bodyFatSavedMessage = ""
                        },
                        label = "Waist",
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedGender == "Female") {
                        Spacer(modifier = Modifier.width(8.dp))

                        MeasurementField(
                            value = hip,
                            onValueChange = {
                                hip = it
                                bodyFatSavedMessage = ""
                            },
                            label = "Hip",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val bodyFatWasAlreadySavedToday = hasBodyFatLogToday

                        val bodyFatValue = String.format(
                            Locale.US,
                            "%.1f",
                            estimatedBodyFat ?: 0.0
                        )

                        createOrUpdateTodayEntry(
                            bodyFatValue = bodyFatValue,
                            updateBodyFat = true
                        )

                        sharedPreferences.edit()
                            .putString("height", height)
                            .putString("neck", neck)
                            .putString("waist", waist)
                            .putString("hip", hip)
                            .putString("latest_body_fat", bodyFatValue)
                            .apply()

                        bodyFatSavedMessage = if (bodyFatWasAlreadySavedToday) {
                            "Body fat log updated."
                        } else {
                            "Body fat log saved."
                        }

                        measurementsSavedMessage = ""
                        historyMessage = ""

                        calculateAndSaveFormScores(context)
                        syncProgressDataToFirebase(context)
                        syncUserDataToFirebase(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    enabled = canSaveBodyFat,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save Body Fat Log",
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = if (hasBodyFatLogToday) {
                            "Update Body Fat Log"
                        } else {
                            "Save Body Fat Log"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (bodyFatSavedMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bodyFatSavedMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProgressSectionCard(
            icon = Icons.Filled.PhotoCamera,
            title = "Progress Photos"
        ) {
            Text(
                text = "Use photos to compare visual changes over time.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (progressPhotos.isEmpty()) {
                EmptyProgressState("No progress photos yet.")
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    progressPhotos.take(3).forEach { photo ->
                        val photoPath = photo.first
                        val photoDate = photo.second
                        val file = File(photoPath)
                        val bitmap = if (file.exists()) {
                            BitmapFactory.decodeFile(photoPath)
                        } else {
                            null
                        }

                        Card(
                            modifier = Modifier
                                .width(118.dp)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(7.dp)
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Progress Photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(13.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    EmptyProgressState("Photo missing")
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = photoDate,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onPhotoCheckInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = "Open Photo Check-In",
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "Open Photo Check-In",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProgressSectionCard(
            icon = Icons.Filled.MonitorWeight,
            title = "Measurements"
        ) {
            Text(
                text = "Save body measurements to today’s history log.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            MeasurementField(
                value = weight,
                onValueChange = {
                    weight = it
                    measurementsSavedMessage = ""
                },
                label = "Weight (kg)"
            )

            if (selectedGender != "Female") {
                MeasurementField(
                    value = hip,
                    onValueChange = {
                        hip = it
                        measurementsSavedMessage = ""
                    },
                    label = "Hip (cm)"
                )
            }

            MeasurementField(
                value = chest,
                onValueChange = {
                    chest = it
                    measurementsSavedMessage = ""
                },
                label = "Chest (cm)"
            )

            MeasurementField(
                value = shoulders,
                onValueChange = {
                    shoulders = it
                    measurementsSavedMessage = ""
                },
                label = "Shoulders (cm)"
            )

            MeasurementField(
                value = arm,
                onValueChange = {
                    arm = it
                    measurementsSavedMessage = ""
                },
                label = "Arm (cm)"
            )

            MeasurementField(
                value = forearm,
                onValueChange = {
                    forearm = it
                    measurementsSavedMessage = ""
                },
                label = "Forearm (cm)"
            )

            MeasurementField(
                value = thigh,
                onValueChange = {
                    thigh = it
                    measurementsSavedMessage = ""
                },
                label = "Thigh (cm)"
            )

            MeasurementField(
                value = calf,
                onValueChange = {
                    calf = it
                    measurementsSavedMessage = ""
                },
                label = "Calf (cm)"
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    createOrUpdateTodayEntry(
                        bodyFatValue = "-",
                        updateBodyFat = false
                    )

                    sharedPreferences.edit()
                        .putString("height", height)
                        .putString("weight", weight)
                        .putString("neck", neck)
                        .putString("waist", waist)
                        .putString("hip", hip)
                        .putString("chest", chest)
                        .putString("shoulders", shoulders)
                        .putString("arm", arm)
                        .putString("forearm", forearm)
                        .putString("thigh", thigh)
                        .putString("calf", calf)
                        .apply()

                    measurementsSavedMessage = "Measurements saved to history."
                    bodyFatSavedMessage = ""
                    historyMessage = ""

                    calculateAndSaveFormScores(context)
                    syncProgressDataToFirebase(context)
                    syncUserDataToFirebase(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save Measurements",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (measurementsSavedMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = measurementsSavedMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProgressSectionHeader(
            icon = Icons.Filled.ShowChart,
            title = "Progress History"
        )

        if (historyMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = historyMessage,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (progressEntries.isEmpty()) {
            EmptyProgressState("No progress logs yet.")
        } else {
            progressEntries.forEachIndexed { index, entry ->
                ProgressHistoryCard(
                    entry = entry,
                    onDeleteClick = {
                        progressEntries.removeAt(index)
                        saveProgressHistory()
                        updateLatestProgressValuesAfterDelete()

                        historyMessage = "Progress log deleted."
                        bodyFatSavedMessage = ""
                        measurementsSavedMessage = ""

                        calculateAndSaveFormScores(context)
                        syncProgressDataToFirebase(context)
                        syncUserDataToFirebase(context)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun ProgressSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ProgressSectionHeader(
                icon = icon,
                title = title
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun ProgressSectionHeader(
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
fun EmptyProgressState(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(11.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProgressHistoryCard(
    entry: ProgressEntry,
    onDeleteClick: () -> Unit
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
            Text(
                text = entry.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (entry.bodyFat.isNotBlank() && entry.bodyFat != "-") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Body Fat",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${entry.bodyFat}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    text = "Measurement Log",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProgressHistoryLine("Height", formatCm(entry.height))
            ProgressHistoryLine("Weight", formatKg(entry.weight))
            ProgressHistoryLine("Neck", formatCm(entry.neck))
            ProgressHistoryLine("Waist", formatCm(entry.waist))
            ProgressHistoryLine("Hip", formatCm(entry.hip))

            Spacer(modifier = Modifier.height(5.dp))

            ProgressHistoryLine("Chest", formatCm(entry.chest))
            ProgressHistoryLine("Shoulders", formatCm(entry.shoulders))
            ProgressHistoryLine("Arm", formatCm(entry.arm))
            ProgressHistoryLine("Forearm", formatCm(entry.forearm))
            ProgressHistoryLine("Thigh", formatCm(entry.thigh))
            ProgressHistoryLine("Calf", formatCm(entry.calf))

            Spacer(modifier = Modifier.height(9.dp))

            OutlinedButton(
                onClick = onDeleteClick,
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

fun formatCm(value: String): String {
    return if (value.isBlank()) "-" else "$value cm"
}

fun formatKg(value: String): String {
    return if (value.isBlank()) "-" else "$value kg"
}

@Composable
fun ProgressHistoryLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
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

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(decimalOnly(newValue))
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}