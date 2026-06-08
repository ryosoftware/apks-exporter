package com.ryosoftware.apks_exporter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContent {
            AppTheme(this@MainActivity) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate("preferences") },
                            onNavigateToAppDetail = { packageName ->
                                navController.navigate("app_detail/$packageName")
                            }
                        )
                    }
                    composable("preferences") {
                        PreferencesScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "app_detail/{packageName}",
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
                        AppDetailScreen(
                            packageName = packageName,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.isSelecting.value) finish()
                else viewModel.cancelSelection()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
        MainBackupWorker.onAppExecuted(this)
    }
}
