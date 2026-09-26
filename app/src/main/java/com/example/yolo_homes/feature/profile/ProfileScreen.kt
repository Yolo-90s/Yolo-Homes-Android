package com.example.yolo_homes.feature.profile

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.AppUpdateChecker
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.ui.components.InitialsAvatar
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.SurfaceCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    session: UserSession,
    appVersion: String,
    // Null when reached from the bottom nav (a top-level destination, no
    // "back" to go to) — non-null if ever pushed on top of something else.
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val darkPref by themeViewModel.darkMode.collectAsStateWithLifecycle()
    val isDark = darkPref ?: isSystemInDarkTheme()
    var checkingForUpdate by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx as? Activity }
    val updateScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InitialsAvatar(session.user.initials, session.user.avatarColor, size = 64.dp)
                Column {
                    Text(session.user.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        session.user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        session.role.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        SurfaceCard {
            Column {
                InfoRow(Icons.Outlined.Shield, "Role", session.role.label)
                InfoRow(Icons.Outlined.Info, "Joined", Formatters.shortDate(session.user.createdAt))
            }
        }

        SectionHeader("Settings")
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Dark Mode",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                Switch(checked = isDark, onCheckedChange = { themeViewModel.setDarkMode(it) })
            }
        }

        SurfaceCard {
            Column {
                Text("About App", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Yolo-Home's — Smart Apartment Management",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Version $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SurfaceCard(
            Modifier.clickable(enabled = !checkingForUpdate) {
                val act = activity ?: return@clickable
                checkingForUpdate = true
                updateScope.launch {
                    val result = AppUpdateChecker.forceCheck(act)
                    checkingForUpdate = false
                    val msg = when (result) {
                        is AppUpdateChecker.Status.UpToDate -> "You're on the latest version."
                        is AppUpdateChecker.Status.UpdatePending ->
                            "Update v${result.versionName} available — follow the prompt to install."
                        is AppUpdateChecker.Status.Failed -> "Couldn't check for updates: ${result.message}"
                    }
                    Toast.makeText(act, msg, Toast.LENGTH_LONG).show()
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (checkingForUpdate) "Checking…" else "Check for updates",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text("  Logout")
        }
    }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
