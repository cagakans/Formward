package com.formward.app.screens

import com.formward.app.R
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formward.app.firebase.syncNutritionDataToFirebase
import com.formward.app.firebase.syncProgressDataToFirebase
import com.formward.app.firebase.syncUserDataToFirebase
import com.formward.app.score.calculateAndSaveFormScores

@Composable
fun InitialSetupScreen(
    onSetupComplete: () -> Unit
) {
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

    var height by remember {
        mutableStateOf(formPreferences.getString("height", "") ?: "")
    }

    var weight by remember {
        mutableStateOf(formPreferences.getString("weight", "") ?: "")
    }

    var age by remember {
        mutableStateOf(nutritionPreferences.getString("age", "") ?: "")
    }

    var genderMenuExpanded by remember { mutableStateOf(false) }
    var goalMenuExpanded by remember { mutableStateOf(false) }
    var activityMenuExpanded by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }

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

    fun setupValidationMessage(): String {
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
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
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.formward_logo),
                    contentDescription = "Formward Logo",
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome to Formward",
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Forward Your Form",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Set up your profile once. You can edit it later.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SetupSectionCard(
            icon = Icons.Filled.Person,
            title = "Setup Details"
        ) {
            Text(
                text = "Basic Information",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { genderMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
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
                                message = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SetupNumberField(
                value = age,
                onValueChange = {
                    age = numberOnly(it)
                    message = ""
                },
                label = "Age",
                keyboardType = KeyboardType.Number
            )

            SetupNumberField(
                value = height,
                onValueChange = {
                    height = decimalOnly(it)
                    message = ""
                },
                label = "Height (cm)",
                keyboardType = KeyboardType.Decimal
            )

            SetupNumberField(
                value = weight,
                onValueChange = {
                    weight = decimalOnly(it)
                    message = ""
                },
                label = "Weight (kg)",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = "Goal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Goal & Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                                message = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                                message = ""
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
                    modifier = Modifier.padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Setup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "These values personalize nutrition targets, progress tracking and daily focus.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val validation = setupValidationMessage()

                if (validation.isNotBlank()) {
                    message = validation
                    return@Button
                }

                formPreferences.edit()
                    .putString("gender", selectedGender)
                    .putString("height", height)
                    .putString("weight", weight)
                    .putString("goal", selectedGoal)
                    .putBoolean("initial_setup_completed", true)
                    .apply()

                nutritionPreferences.edit()
                    .putString("age", age)
                    .putString("activity_level", activityLevel)
                    .apply()

                calculateAndSaveFormScores(context)
                syncProgressDataToFirebase(context)
                syncNutritionDataToFirebase(context)
                syncUserDataToFirebase(context)

                message = "Setup completed."
                onSetupComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Complete Setup",
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Complete Setup",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
fun SetupSectionCard(
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
fun SetupNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}