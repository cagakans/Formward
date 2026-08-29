package com.example.mis49mproject.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.mis49mproject.firebase.syncNutritionDataToFirebase
import com.example.mis49mproject.firebase.syncUserDataToFirebase
import com.example.mis49mproject.score.calculateAndSaveFormScores
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current

    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val savedNutritionDate = nutritionPreferences.getString("nutrition_date", "") ?: ""
    val isTodayNutrition = savedNutritionDate == todayDate

    val gender = formPreferences.getString("gender", "") ?: ""
    val height = formPreferences.getString("height", "") ?: ""
    val weight = formPreferences.getString("weight", "") ?: ""
    val goal = formPreferences.getString("goal", "") ?: ""

    val age = nutritionPreferences.getString("age", "") ?: ""
    val activityLevel = nutritionPreferences.getString("activity_level", "") ?: ""

    var calorieTarget by remember {
        mutableStateOf(nutritionPreferences.getString("calorie_target", "") ?: "")
    }

    var proteinTarget by remember {
        mutableStateOf(nutritionPreferences.getString("protein_target", "") ?: "")
    }

    var carbsTarget by remember {
        mutableStateOf(nutritionPreferences.getString("carbs_target", "") ?: "")
    }

    var fatTarget by remember {
        mutableStateOf(nutritionPreferences.getString("fat_target", "") ?: "")
    }

    var waterTarget by remember {
        mutableStateOf(nutritionPreferences.getString("water_target", "") ?: "")
    }

    var calorieConsumed by remember {
        mutableStateOf(
            if (isTodayNutrition) {
                nutritionPreferences.getString("calorie_consumed", "") ?: ""
            } else {
                ""
            }
        )
    }

    var proteinConsumed by remember {
        mutableStateOf(
            if (isTodayNutrition) {
                nutritionPreferences.getString("protein_consumed", "") ?: ""
            } else {
                ""
            }
        )
    }

    var carbsConsumed by remember {
        mutableStateOf(
            if (isTodayNutrition) {
                nutritionPreferences.getString("carbs_consumed", "") ?: ""
            } else {
                ""
            }
        )
    }

    var fatConsumed by remember {
        mutableStateOf(
            if (isTodayNutrition) {
                nutritionPreferences.getString("fat_consumed", "") ?: ""
            } else {
                ""
            }
        )
    }

    var waterConsumed by remember {
        mutableStateOf(
            if (isTodayNutrition) {
                nutritionPreferences.getString("water_consumed", "") ?: ""
            } else {
                ""
            }
        )
    }

    var targetSavedMessage by remember { mutableStateOf("") }
    var intakeSavedMessage by remember { mutableStateOf("") }
    var calculationMessage by remember { mutableStateOf("") }

    fun toDouble(value: String): Double {
        return value.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    }

    fun activityMultiplier(level: String): Double {
        return when (level) {
            "Mostly sitting" -> 1.20
            "Light activity" -> 1.375
            "Moderate training" -> 1.55
            "Active lifestyle" -> 1.725
            "Very active" -> 1.90
            else -> 0.0
        }
    }

    fun proteinMultiplier(goalValue: String): Double {
        return when (goalValue) {
            "Fat Loss" -> 2.0
            "Muscle Gain" -> 1.8
            "Body Recomposition" -> 2.0
            "Strength" -> 1.8
            "General Fitness" -> 1.4
            else -> 1.6
        }
    }

    fun goalCalorieAdjustment(goalValue: String): Int {
        return when (goalValue) {
            "Fat Loss" -> -400
            "Muscle Gain" -> 250
            "Body Recomposition" -> -150
            "Strength" -> 100
            "General Fitness" -> 0
            else -> 0
        }
    }

    fun calculateNutritionScore(): Int {
        var score = 0

        val calorieTargetValue = toDouble(calorieTarget)
        val calorieConsumedValue = toDouble(calorieConsumed)

        val proteinTargetValue = toDouble(proteinTarget)
        val proteinConsumedValue = toDouble(proteinConsumed)

        val waterTargetValue = toDouble(waterTarget)
        val waterConsumedValue = toDouble(waterConsumed)

        if (calorieTargetValue > 0 && calorieConsumedValue > 0) {
            val ratio = calorieConsumedValue / calorieTargetValue
            score += if (ratio in 0.80..1.10) 30 else 15
        }

        if (proteinTargetValue > 0 && proteinConsumedValue > 0) {
            val ratio = proteinConsumedValue / proteinTargetValue
            score += if (ratio >= 0.80) 30 else 15
        }

        if (waterTargetValue > 0 && waterConsumedValue > 0) {
            val ratio = waterConsumedValue / waterTargetValue
            score += if (ratio >= 0.80) 25 else 10
        }

        if (carbsConsumed.isNotBlank() && fatConsumed.isNotBlank()) {
            score += 15
        }

        return score.coerceIn(0, 100)
    }

    fun autoCalculateTargets() {
        val ageValue = toDouble(age)
        val heightValue = toDouble(height)
        val weightValue = toDouble(weight)
        val activityValue = activityMultiplier(activityLevel)

        targetSavedMessage = ""
        intakeSavedMessage = ""

        if (gender.isBlank() || heightValue <= 0 || weightValue <= 0 || goal.isBlank()) {
            calculationMessage = "Complete gender, height, weight and goal from Profile first."
            return
        }

        if (ageValue <= 0) {
            calculationMessage = "Complete your age from Profile first."
            return
        }

        if (activityValue <= 0) {
            calculationMessage = "Choose your activity level from Profile first."
            return
        }

        val bmr = if (gender == "Male") {
            10 * weightValue + 6.25 * heightValue - 5 * ageValue + 5
        } else {
            10 * weightValue + 6.25 * heightValue - 5 * ageValue - 161
        }

        val tdee = bmr * activityValue
        val targetCalories = (tdee + goalCalorieAdjustment(goal))
            .roundToInt()
            .coerceAtLeast(1200)

        val proteinGrams = (weightValue * proteinMultiplier(goal)).roundToInt()

        val fatCalories = targetCalories * 0.25
        val fatGrams = (fatCalories / 9.0).roundToInt()

        val proteinCalories = proteinGrams * 4
        val carbCalories = targetCalories - proteinCalories - fatCalories
        val carbGrams = (carbCalories / 4.0).roundToInt().coerceAtLeast(0)

        val waterLiters = weightValue * 35.0 / 1000.0

        calorieTarget = targetCalories.toString()
        proteinTarget = proteinGrams.toString()
        carbsTarget = carbGrams.toString()
        fatTarget = fatGrams.toString()
        waterTarget = String.format(Locale.US, "%.1f", waterLiters)

        calculationMessage = "Targets calculated. Tap Save Targets to keep them."
    }

    val nutritionScore = calculateNutritionScore()

    val scoreMessage = when {
        nutritionScore >= 85 -> "Excellent tracking"
        nutritionScore >= 60 -> "Good progress"
        nutritionScore > 0 -> "Complete more fields"
        else -> "Enter today's intake"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Nutrition",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Set targets and track daily intake.",
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
                            text = "Today's Nutrition Score",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$nutritionScore",
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
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = "Nutrition Score",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$scoreMessage • $nutritionScore / 100",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(9.dp))

                LinearProgressIndicator(
                    progress = { nutritionScore / 100f },
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

        NutritionSectionCard(
            icon = Icons.Filled.Calculate,
            title = "Targets"
        ) {
            Text(
                text = "Auto-calculate targets from your saved profile details, or edit them manually.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Need to change age, activity, weight or goal?",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onProfileClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Edit Profile →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    autoCalculateTargets()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Auto Calculate Targets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (calculationMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = calculationMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manual Targets",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            NutritionNumberField(
                value = calorieTarget,
                onValueChange = {
                    calorieTarget = it
                    targetSavedMessage = ""
                    calculationMessage = ""
                },
                label = "Calorie Target (kcal)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = proteinTarget,
                onValueChange = {
                    proteinTarget = it
                    targetSavedMessage = ""
                    calculationMessage = ""
                },
                label = "Protein Target (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = carbsTarget,
                onValueChange = {
                    carbsTarget = it
                    targetSavedMessage = ""
                    calculationMessage = ""
                },
                label = "Carbs Target (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = fatTarget,
                onValueChange = {
                    fatTarget = it
                    targetSavedMessage = ""
                    calculationMessage = ""
                },
                label = "Fat Target (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = waterTarget,
                onValueChange = {
                    waterTarget = it
                    targetSavedMessage = ""
                    calculationMessage = ""
                },
                label = "Water Target (L)",
                allowDecimal = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    nutritionPreferences.edit()
                        .putString("calorie_target", calorieTarget)
                        .putString("protein_target", proteinTarget)
                        .putString("carbs_target", carbsTarget)
                        .putString("fat_target", fatTarget)
                        .putString("water_target", waterTarget)
                        .apply()

                    calculationMessage = ""
                    targetSavedMessage = "Targets saved."
                    intakeSavedMessage = ""

                    syncNutritionDataToFirebase(context)
                    calculateAndSaveFormScores(context)
                    syncUserDataToFirebase(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save Targets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (targetSavedMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = targetSavedMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        NutritionSectionCard(
            icon = Icons.Filled.LocalDrink,
            title = "Today's Intake"
        ) {
            Text(
                text = "These fields reset daily.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            NutritionNumberField(
                value = calorieConsumed,
                onValueChange = {
                    calorieConsumed = it
                    intakeSavedMessage = ""
                },
                label = "Calories Consumed (kcal)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = proteinConsumed,
                onValueChange = {
                    proteinConsumed = it
                    intakeSavedMessage = ""
                },
                label = "Protein Consumed (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = carbsConsumed,
                onValueChange = {
                    carbsConsumed = it
                    intakeSavedMessage = ""
                },
                label = "Carbs Consumed (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = fatConsumed,
                onValueChange = {
                    fatConsumed = it
                    intakeSavedMessage = ""
                },
                label = "Fat Consumed (g)",
                allowDecimal = true
            )

            NutritionNumberField(
                value = waterConsumed,
                onValueChange = {
                    waterConsumed = it
                    intakeSavedMessage = ""
                },
                label = "Water Consumed (L)",
                allowDecimal = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                nutritionPreferences.edit()
                    .putString("nutrition_date", todayDate)
                    .putString("calorie_target", calorieTarget)
                    .putString("calorie_consumed", calorieConsumed)
                    .putString("protein_target", proteinTarget)
                    .putString("protein_consumed", proteinConsumed)
                    .putString("carbs_target", carbsTarget)
                    .putString("carbs_consumed", carbsConsumed)
                    .putString("fat_target", fatTarget)
                    .putString("fat_consumed", fatConsumed)
                    .putString("water_target", waterTarget)
                    .putString("water_consumed", waterConsumed)
                    .putInt("nutrition_score", nutritionScore)
                    .apply()

                intakeSavedMessage = "Today's intake saved."
                targetSavedMessage = ""
                calculationMessage = ""

                syncNutritionDataToFirebase(context)
                calculateAndSaveFormScores(context)
                syncUserDataToFirebase(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = "Save Today's Intake",
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "Save Today's Intake",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (intakeSavedMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = intakeSavedMessage,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun NutritionSectionCard(
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun NutritionNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    allowDecimal: Boolean
) {
    fun filterInput(input: String): String {
        if (!allowDecimal) {
            return input.filter { it.isDigit() }
        }

        val cleaned = input
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
            onValueChange(filterInput(newValue))
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Number
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}