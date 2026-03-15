package com.openbiblescholar.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openbiblescholar.ui.screens.home.HomeScreen
import com.openbiblescholar.ui.screens.library.LibraryScreen
import com.openbiblescholar.ui.screens.onboarding.OnboardingScreen
import com.openbiblescholar.ui.screens.reader.BibleReaderScreen
import com.openbiblescholar.ui.screens.settings.SettingsScreen
import com.openbiblescholar.ui.screens.study.StudyCenterScreen
import com.openbiblescholar.ui.screens.study.WordStudyScreen
import com.openbiblescholar.ui.screens.study.PassageGuideScreen
import com.openbiblescholar.ui.screens.settings.ApiKeySettingsScreen
import com.openbiblescholar.ui.screens.settings.ReadingPlanScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object BibleReader : Screen("reader/{book}/{chapter}/{verse}") {
        fun createRoute(book: String, chapter: Int, verse: Int = 1) =
            "reader/$book/$chapter/$verse"
    }
    object StudyCenter : Screen("study/{book}/{chapter}/{verse}") {
        fun createRoute(book: String, chapter: Int, verse: Int) =
            "study/$book/$chapter/$verse"
    }
    object WordStudy : Screen("word_study/{word}/{strongs}") {
        fun createRoute(word: String, strongs: String) = "word_study/$word/$strongs"
    }
    object PassageGuide : Screen("passage_guide/{book}/{chapter}") {
        fun createRoute(book: String, chapter: Int) = "passage_guide/$book/$chapter"
    }
    object Library : Screen("library")
    object Settings : Screen("settings")
    object ApiKeySettings : Screen("settings/api_keys")
    object ReadingPlan : Screen("reading_plan")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                animationSpec = tween(200), initialOffsetX = { it / 4 }
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                animationSpec = tween(200), targetOffsetX = { -it / 4 }
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                animationSpec = tween(200), initialOffsetX = { -it / 4 }
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                animationSpec = tween(200), targetOffsetX = { it / 4 }
            )
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenReader = { book, chapter, verse ->
                    navController.navigate(Screen.BibleReader.createRoute(book, chapter, verse))
                },
                onOpenLibrary = { navController.navigate(Screen.Library.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenReadingPlan = { navController.navigate(Screen.ReadingPlan.route) }
            )
        }

        composable(
            route = Screen.BibleReader.route,
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType },
                navArgument("verse") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStack ->
            val book = backStack.arguments?.getString("book") ?: "Genesis"
            val chapter = backStack.arguments?.getInt("chapter") ?: 1
            val verse = backStack.arguments?.getInt("verse") ?: 1

            BibleReaderScreen(
                initialBook = book,
                initialChapter = chapter,
                initialVerse = verse,
                onNavigateUp = { navController.navigateUp() },
                onOpenStudyCenter = { b, c, v ->
                    navController.navigate(Screen.StudyCenter.createRoute(b, c, v))
                },
                onOpenWordStudy = { word, strongs ->
                    navController.navigate(Screen.WordStudy.createRoute(word, strongs))
                },
                onOpenPassageGuide = { b, c ->
                    navController.navigate(Screen.PassageGuide.createRoute(b, c))
                },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.StudyCenter.route,
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType },
                navArgument("verse") { type = NavType.IntType }
            )
        ) { backStack ->
            StudyCenterScreen(
                book = backStack.arguments?.getString("book") ?: "Genesis",
                chapter = backStack.arguments?.getInt("chapter") ?: 1,
                verse = backStack.arguments?.getInt("verse") ?: 1,
                onNavigateUp = { navController.navigateUp() },
                onOpenWordStudy = { word, strongs ->
                    navController.navigate(Screen.WordStudy.createRoute(word, strongs))
                }
            )
        }

        composable(
            route = Screen.WordStudy.route,
            arguments = listOf(
                navArgument("word") { type = NavType.StringType },
                navArgument("strongs") { type = NavType.StringType }
            )
        ) { backStack ->
            WordStudyScreen(
                word = backStack.arguments?.getString("word") ?: "",
                strongsNumber = backStack.arguments?.getString("strongs") ?: "",
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.PassageGuide.route,
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType }
            )
        ) { backStack ->
            PassageGuideScreen(
                book = backStack.arguments?.getString("book") ?: "Genesis",
                chapter = backStack.arguments?.getInt("chapter") ?: 1,
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onOpenApiKeys = { navController.navigate(Screen.ApiKeySettings.route) }
            )
        }

        composable(Screen.ApiKeySettings.route) {
            ApiKeySettingsScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.ReadingPlan.route) {
            ReadingPlanScreen(
                onNavigateUp = { navController.navigateUp() },
                onOpenReader = { book, chapter ->
                    navController.navigate(Screen.BibleReader.createRoute(book, chapter, 1))
                }
            )
        }
    }
}
