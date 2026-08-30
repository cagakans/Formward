package com.formward.app.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.formward.app.screens.CheckInScreen
import com.formward.app.screens.HomeScreen
import com.formward.app.screens.InitialSetupScreen
import com.formward.app.screens.NutritionScreen
import com.formward.app.screens.PhotoCheckInScreen
import com.formward.app.screens.ProfileScreen
import com.formward.app.screens.ProgressScreen
import com.formward.app.screens.WorkoutLogScreen
import androidx.compose.material.icons.filled.Assignment

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val formPreferences = context.getSharedPreferences("formward_data", Context.MODE_PRIVATE)

    var setupCompleted by remember {
        mutableStateOf(formPreferences.getBoolean("initial_setup_completed", false))
    }

    val bottomNavItems = listOf(
        BottomNavItem("home", "Home", Icons.Filled.Home),
        BottomNavItem("checkin", "Mission", Icons.Filled.Assignment),
        BottomNavItem("workout", "Workout", Icons.Filled.FitnessCenter),
        BottomNavItem("nutrition", "Nutrition", Icons.Filled.Restaurant),
        BottomNavItem("progress", "Progress", Icons.Filled.ShowChart)
    )

    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    fun navigateToBottomRoute(route: String) {
        if (currentRoute == route) return

        navController.navigate(route) {
            popUpTo("home") {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("photo_checkin") {
                            launchSingleTop = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Photo Check-In"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            if (currentRoute != "setup") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navigateToBottomRoute(item.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(item.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = if (setupCompleted) "home" else "setup",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("setup") {
                InitialSetupScreen(
                    onSetupComplete = {
                        setupCompleted = true

                        navController.navigate("home") {
                            popUpTo("setup") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    onProfileClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    },
                    onMissionClick = {
                        navController.navigate("checkin") {
                            launchSingleTop = true
                        }
                    },
                    onWorkoutClick = {
                        navController.navigate("workout") {
                            launchSingleTop = true
                        }
                    },
                    onNutritionClick = {
                        navController.navigate("nutrition") {
                            launchSingleTop = true
                        }
                    },
                    onProgressClick = {
                        navController.navigate("progress") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("checkin") {
                CheckInScreen()
            }

            composable("workout") {
                WorkoutLogScreen()
            }

            composable("nutrition") {
                NutritionScreen(
                    onProfileClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("progress") {
                ProgressScreen(
                    onPhotoCheckInClick = {
                        navController.navigate("photo_checkin") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("profile") {
                ProfileScreen()
            }

            composable("photo_checkin") {
                PhotoCheckInScreen()
            }
        }
    }
}