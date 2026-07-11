package com.example.unimove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.unimove.ui.screens.auth.LandingScreen
import com.example.unimove.ui.screens.auth.LoginScreen
import com.example.unimove.ui.screens.auth.RegisterScreen
import com.example.unimove.ui.screens.auth.SplashScreen
import com.example.unimove.ui.screens.onboarding.OnboardingHostScreen
import com.example.unimove.ui.screens.profile.ProfileScreen
import com.example.unimove.ui.screens.*
import com.example.unimove.ui.theme.UniMoveTheme
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.viewmodel.MealLogViewModel
import com.example.unimove.ui.viewmodel.RunViewModel
import com.example.unimove.ui.viewmodel.FitBotViewModel
import com.example.unimove.ui.monster.EventRunScreen
import com.example.unimove.ui.monster.MonsterEventScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniMoveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

//This is the app entry point. MainActivity loads the Compose theme and calls AppNavigation().
// All major screens are registered in the NavHost.

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val runViewModel: RunViewModel = viewModel()
    val mealLogViewModel: MealLogViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        // ── Auth ───────────────────────────────────────────────
        composable("splash") {
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("landing") {
            LandingScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("login") {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }

        // ── Onboarding ─────────────────────────────────────────
        composable("onboarding") {
            OnboardingHostScreen(navController = navController)
        }

        // ── Main App ───────────────────────────────────────────
        composable("home") {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel,
                mealLogViewModel = mealLogViewModel
            )
        }
        composable("meal_log") {
            MealLogScreen(
                navController = navController,
                mealLogViewModel = mealLogViewModel,
                authViewModel = authViewModel
            )
        }
        composable("monster_event") {
            MonsterEventScreen(
                onBack = {
                    navController.popBackStack()
                },
                onStartEvent = {
                    navController.navigate("event_run")
                }
            )
        }
        composable("event_run") {
            EventRunScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            "run?challengeId={challengeId}&challengeTitle={challengeTitle}&targetDistanceKm={targetDistanceKm}",
            arguments = listOf(navArgument("challengeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }, navArgument("challengeTitle") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }, navArgument("targetDistanceKm") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")
            val challengeTitle = backStackEntry.arguments?.getString("challengeTitle")
            val targetDistanceKm = backStackEntry.arguments?.getString("targetDistanceKm")?.toDoubleOrNull()
            RunScreen(
                navController = navController,
                viewModel = runViewModel,
                challengeId = challengeId,
                challengeTitle = challengeTitle,
                targetDistanceKm = targetDistanceKm
            )
        }
        composable("challenge") {
            ChallengeScreen(
                navController = navController,
                runViewModel = runViewModel
            )
        }
        composable(
            "challenge_result/{challengeId}?viewOnly={viewOnly}",
            arguments = listOf(
                navArgument("challengeId") { type = NavType.StringType },
                navArgument("viewOnly") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId").orEmpty()
            val viewOnly = backStackEntry.arguments?.getBoolean("viewOnly") ?: false
            ChallengeResultScreen(
                navController = navController,
                viewModel = runViewModel,
                challengeId = challengeId,
                viewOnly = viewOnly
            )
        }
        composable("leaderboard") {
            LeaderboardScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("achievement") {
            AchievementScreen(navController = navController)
        }
        composable("community") {
            CommunityFeedScreen(navController = navController)
        }
        composable(
            "session_summary?challengeId={challengeId}",
            arguments = listOf(navArgument("challengeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")
            SessionSummaryScreen(
                navController = navController,
                viewModel = runViewModel,
                authViewModel = authViewModel,
                challengeId = challengeId
            )
        }

        // ── Placeholders ───────────────────────────────────────
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("fitbot") {
            val fitBotViewModel: FitBotViewModel = viewModel()
            FitBotScreen(
                navController = navController,
                authViewModel = authViewModel,
                fitBotViewModel = fitBotViewModel
            )
        }
    }
}
