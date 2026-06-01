package com.example.yolo_homes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.feature.auth.AuthUiState
import com.example.yolo_homes.feature.auth.AuthViewModel
import com.example.yolo_homes.feature.auth.LoginScreen
import com.example.yolo_homes.feature.auth.SplashScreen
import com.example.yolo_homes.feature.profile.ThemeViewModel
import com.example.yolo_homes.ui.navigation.YoloNavGraph
import com.example.yolo_homes.ui.theme.YoloHomesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { YoloHomesRoot() }
    }
}

@Composable
private fun YoloHomesRoot(
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val darkPref by themeViewModel.darkMode.collectAsStateWithLifecycle()
    val darkTheme = darkPref ?: isSystemInDarkTheme()

    // Hold the boot animation on screen long enough to play through, even if auth resolves instantly.
    var bootDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2700)
        bootDone = true
    }
    val showSplash = authState is AuthUiState.Loading || !bootDone

    YoloHomesTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = showSplash,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                label = "boot"
            ) { splash ->
                if (splash) {
                    SplashScreen()
                } else {
                    when (val state = authState) {
                        is AuthUiState.SignedIn -> YoloNavGraph(
                            session = state.session,
                            appVersion = BuildConfig.VERSION_NAME,
                            onLogout = { authViewModel.signOut() }
                        )
                        else -> LoginScreen(authViewModel)
                    }
                }
            }
        }
    }
}
