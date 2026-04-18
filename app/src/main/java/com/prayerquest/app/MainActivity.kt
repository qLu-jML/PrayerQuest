package com.prayerquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.ui.navigation.PrayerQuestNavHost
import com.prayerquest.app.ui.onboarding.OnboardingScreen
import com.prayerquest.app.ui.onboarding.OnboardingViewModel
import com.prayerquest.app.ui.theme.LocalIsPremium
import com.prayerquest.app.ui.theme.PrayerQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PrayerQuestApplication
        val container = app.container
        val userPreferences = container.userPreferences

        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val selectedThemeId by userPreferences.selectedThemeId.collectAsState(initial = "prayer_quest")
            val onboardingCompleted by userPreferences.onboardingCompleted.collectAsState(initial = null)

            // Premium entitlement, combined from Play Billing + DataStore
            // mirror. Provided as a CompositionLocal so any composable (banner
            // ads, gated feature buttons, paywall chrome) can read it without
            // having to plumb it through every screen signature.
            val isPremium by container.premiumRepository.isPremium.collectAsState(initial = false)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PrayerQuestTheme(
                themeId = selectedThemeId,
                darkTheme = darkTheme
            ) {
              CompositionLocalProvider(LocalIsPremium provides isPremium) {
                when (onboardingCompleted) {
                    // Still loading from DataStore — show brief splash
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // First launch — show the tutorial / onboarding flow
                    false -> {
                        val onboardingViewModel: OnboardingViewModel = viewModel(
                            factory = OnboardingViewModel.Factory(
                                collectionRepository = container.collectionRepository,
                                gratitudeRepository = container.gratitudeRepository,
                                userPreferences = userPreferences,
                                applicationContext = applicationContext
                            )
                        )
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            onNavigateToHome = {
                                // Force recompose by letting the flow emit true
                                // (setOnboardingCompleted already called in ViewModel)
                            }
                        )
                    }

                    // Onboarding done — show main app
                    true -> {
                        PrayerQuestNavHost(
                            userPreferences = userPreferences
                        )
                    }
                }
              } // CompositionLocalProvider
            } // PrayerQuestTheme
        } // setContent
    }
}
