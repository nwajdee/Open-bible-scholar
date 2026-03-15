package com.openbiblescholar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.openbiblescholar.ui.navigation.AppNavGraph
import com.openbiblescholar.ui.theme.OpenBibleTheme
import com.openbiblescholar.ui.screens.onboarding.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val onboardingVm: OnboardingViewModel = hiltViewModel()
            val appReady by onboardingVm.appReady.collectAsState()

            splashScreen.setKeepOnScreenCondition { !appReady }

            OpenBibleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (appReady) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
