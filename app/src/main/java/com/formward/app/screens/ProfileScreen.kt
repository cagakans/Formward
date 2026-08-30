package com.formward.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formward.app.firebase.syncNutritionDataToFirebase
import com.formward.app.firebase.syncProgressDataToFirebase
import com.formward.app.firebase.syncUserDataToFirebase
import com.formward.app.score.calculateAndSaveFormScores

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)
    val nutritionPreferences = context.getSharedPreferences("nutrition_data", Context.MODE_PRIVATE)

    var selectedGender by remember {
        mutableStateOf(formPreferences.getString("gender", null))
    }

    var selectedGoal by remember {
        mutableStateOf(formPreferences.getString("goal", null))
    }

    var activityLevel by remember {
        mutableStateOf(nutritionPreferences.getString("activity_level", null))
    }

    var age by remember {
        mutableStateOf(nutritionPreferences.getString("age", "") ?: "")
    }

    var height by remember {
        mutableStateOf(formPreferences.getString("height", "") ?: "")
    }

    var weight by remember {
        mutableStateOf(formPreferences.getString("weight", "") ?: "")
    }

    var genderMenuExpanded by remember { mutableStateOf(false) }
    var goalMenuExpanded by remember { mutableStateOf(false) }
    var activityMenuExpanded by remember { mutableStateOf(false) }

    var savedMessage by remember { mutableStateOf("") }

    val weeklyScore = formPreferences.getInt("weekly_form_score", 0)
    val streakCount = formPreferences.getInt("streak_count", 0)

    val latestBodyFat = formPreferences.getString("latest_body_fat", "-") ?: "-"
    val progressHistoryData = formPreferences.getString("progress_history_v1", "") ?: ""
    val isGenderLocked = progressHistoryData.isNotBlank()

    val level = when {
        weeklyScore >= 90 -> "Elite"
        weeklyScore >= 75 -> "Advanced"
        weeklyScore >= 50 -> "Consistent"
        weeklyScore > 0 -> "Building"
        else -> "Getting Started"
    }

    val levelMessage = when (level) {
        "Elite" -> "Very strong weekly performance."
        "Advanced" -> "Strong weekly momentum."
        "Consistent" -> "You are staying consistent."
        "Building" -> "You are building discipline."
        else -> "Complete daily actions to build your level."
    }

    val genderOptions = listOf("Male", "Female")

    val goalOptions = listOf(
        "Fat Loss",
        "Muscle Gain",
        "Body Recomposition",
        "Strength",
        "General Fitness"
    )

    val activityOptions = listOf(
        "Mostly sitting",
        "Light activity",
        "Moderate training",
        "Active lifestyle",
        "Very active"
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

    fun toDouble(value: String): Double {
        return value
            .replace(",", ".")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    fun profileValidationMessage(): String {
        val ageValue = toDouble(age)
        val heightValue = toDouble(height)
        val weightValue = toDouble(weight)

        if (selectedGender == null) return "Choose your gender."
        if (age.isBlank()) return "Enter your age."
        if (height.isBlank()) return "Enter your height."
        if (weight.isBlank()) return "Enter your weight."
        if (selectedGoal == null) return "Choose your goal."
        if (activityLevel == null) return "Choose your activity level."

        if (ageValue !in 10.0..100.0) return "Age should be between 10 and 100."
        if (heightValue !in 100.0..250.0) return "Height should be between 100 and 250 cm."
        if (weightValue !in 30.0..300.0) return "Weight should be between 30 and 300 kg."

        return ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Manage your profile and fitness goal.",
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Formward Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = levelMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ProfileMiniStat(
                        title = "Level",
                        value = level,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ProfileMiniStat(
                        title = "Streak",
                        value = "$streakCount days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProfileSectionCard(
            icon = Icons.Filled.Edit,
            title = "Profile Details"
        ) {
            Text(
                text = "Edit your core details for nutrition targets and progress tracking.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Gender",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box {
                OutlinedButton(
                    onClick = { genderMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    enabled = !isGenderLocked,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = selectedGender ?: "Choose Gender",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = genderMenuExpanded,
                    onDismissRequest = { genderMenuExpanded = false }
                ) {
                    genderOptions.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender) },
                            onClick = {
                                selectedGender = gender
                                genderMenuExpanded = false
                                savedMessage = ""
                            }
                        )
                    }
                }
            }

            if (isGenderLocked) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Gender is locked after the first progress log.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileNumberField(
                value = age,
                onValueChange = {
                    age = numberOnly(it)
                    savedMessage = ""
                },
                label = "Age",
                keyboardType = KeyboardType.Number
            )

            ProfileNumberField(
                value = height,
                onValueChange = {
                    height = decimalOnly(it)
                    savedMessage = ""
                },
                label = "Height (cm)",
                keyboardType = KeyboardType.Decimal
            )

            ProfileNumberField(
                value = weight,
                onValueChange = {
                    weight = decimalOnly(it)
                    savedMessage = ""
                },
                label = "Weight (kg)",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Goal",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box {
                OutlinedButton(
                    onClick = { goalMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = selectedGoal ?: "Choose Goal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = goalMenuExpanded,
                    onDismissRequest = { goalMenuExpanded = false }
                ) {
                    goalOptions.forEach { goal ->
                        DropdownMenuItem(
                            text = { Text(goal) },
                            onClick = {
                                selectedGoal = goal
                                goalMenuExpanded = false
                                savedMessage = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Activity Level",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box {
                OutlinedButton(
                    onClick = { activityMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = activityLevel ?: "Choose Activity Level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = activityMenuExpanded,
                    onDismissRequest = { activityMenuExpanded = false }
                ) {
                    activityOptions.forEach { activity ->
                        DropdownMenuItem(
                            text = { Text(activity) },
                            onClick = {
                                activityLevel = activity
                                activityMenuExpanded = false
                                savedMessage = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
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
                        text = if (latestBodyFat == "-") "-" else "$latestBodyFat% • from Progress",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val validation = profileValidationMessage()

                    if (validation.isNotBlank()) {
                        savedMessage = validation
                        return@Button
                    }

                    formPreferences.edit()
                        .putString("gender", selectedGender)
                        .putString("height", height)
                        .putString("weight", weight)
                        .putString("goal", selectedGoal)
                        .apply()

                    nutritionPreferences.edit()
                        .putString("age", age)
                        .putString("activity_level", activityLevel)
                        .apply()

                    calculateAndSaveFormScores(context)
                    syncProgressDataToFirebase(context)
                    syncNutritionDataToFirebase(context)
                    syncUserDataToFirebase(context)

                    savedMessage = "Profile updated."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save Profile",
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "Save Profile",
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
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun ProfileMiniStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProfileSectionCard(
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
        Column(
            modifier = Modifier.padding(14.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun ProfileNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}