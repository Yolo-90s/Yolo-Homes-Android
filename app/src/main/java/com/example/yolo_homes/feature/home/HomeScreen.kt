package com.example.yolo_homes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.data.model.Post
import com.example.yolo_homes.data.model.UserSession
import com.example.yolo_homes.feature.maintenance.MaintenanceViewModel
import com.example.yolo_homes.feature.water.WaterViewModel
import com.example.yolo_homes.ui.components.EmptyState
import com.example.yolo_homes.ui.components.GradientHeader
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.StatCard
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.ChartSky

/**
 * The real landing screen — what a non-technical resident sees first. Answers "what do I do
 * here" directly: a greeting, this month's numbers, shortcuts to everything else, and the
 * Community Board (news/events/notices) — the one section that lives here only, independent
 * of Maintenance/Water.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: UserSession,
    onOpenMaintenance: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenReports: () -> Unit,
    onAddMaintenance: () -> Unit,
    onAddReading: () -> Unit,
    onOpenFlatDirectory: () -> Unit,
    onOpenMyFlat: (String) -> Unit,
    maintenanceViewModel: MaintenanceViewModel = hiltViewModel(),
    waterViewModel: WaterViewModel = hiltViewModel(),
    postsViewModel: PostsViewModel = hiltViewModel()
) {
    val maintenanceState by maintenanceViewModel.uiState.collectAsStateWithLifecycle()
    val waterState by waterViewModel.uiState.collectAsStateWithLifecycle()
    val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
    val postSaveState by postsViewModel.saveState.collectAsStateWithLifecycle()
    val firstName = session.user.displayName.substringBefore(' ').ifBlank { "there" }

    var composing by remember { mutableStateOf(false) }
    var viewingPost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(postSaveState) {
        if (postSaveState is PostSaveState.Saved) {
            composing = false
            postsViewModel.clearSaveState()
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            GradientHeader {
                Text(
                    "Hi, $firstName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    if (session.isAdmin) "Here's what's happening in your building."
                    else "Here's your apartment overview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        "Maintenance This Month", maintenanceState.currentMonthTotal,
                        Icons.Outlined.Payments, MaterialTheme.colorScheme.primary, Modifier.weight(1f)
                    ) { Formatters.currency(it, maintenanceState.currency) }
                    StatCard(
                        "Water This Month", waterState.totalConsumption,
                        Icons.Outlined.WaterDrop, ChartSky, Modifier.weight(1f)
                    ) { Formatters.liters(it) }
                }

                HomeShortcutCard(
                    title = "Maintenance",
                    subtitle = "View payment history & receipts",
                    icon = Icons.Outlined.Receipt,
                    onClick = onOpenMaintenance
                )
                HomeShortcutCard(
                    title = "Water",
                    subtitle = "View usage, bills & meter readings",
                    icon = Icons.Outlined.WaterDrop,
                    onClick = onOpenWater
                )
                HomeShortcutCard(
                    title = "Reports",
                    subtitle = "Download monthly summaries as PDF",
                    icon = Icons.Outlined.Assessment,
                    onClick = onOpenReports
                )
                if (session.isResident && !session.flatId.isNullOrBlank()) {
                    HomeShortcutCard(
                        title = "My Flat",
                        subtitle = "Your unit's full bill history",
                        icon = Icons.Outlined.Home,
                        onClick = { onOpenMyFlat(session.flatId!!) }
                    )
                }
                HomeShortcutCard(
                    title = "Flat Directory",
                    subtitle = if (session.isAdmin) "Manage units, residents & staff" else "See all flats in the building",
                    icon = Icons.Outlined.Apartment,
                    onClick = onOpenFlatDirectory
                )

                if (session.isAdmin) {
                    Text(
                        "Quick add",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onAddMaintenance, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Payment")
                        }
                        OutlinedButton(onClick = onAddReading, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Reading")
                        }
                    }
                }

                SectionHeader(
                    "Community Board",
                    action = if (postsState.isAdmin) {
                        {
                            IconButton(onClick = { composing = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = "New post")
                            }
                        }
                    } else null
                )
                if (postsState.posts.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Apartment,
                        title = "Nothing Posted Yet",
                        message = if (postsState.isAdmin) "Post building news, events or notices here."
                        else "Building news, events & notices will show up here."
                    )
                } else {
                    postsState.posts.forEach { post ->
                        PostCard(post = post, onClick = { viewingPost = post })
                    }
                }
            }
        }
    }

    if (composing) {
        ComposePostSheet(
            saving = postSaveState is PostSaveState.Saving,
            onDismiss = { composing = false },
            onSave = { title, body, type, eventDateMillis, pinned ->
                postsViewModel.createPost(title, body, type, eventDateMillis, pinned)
            }
        )
    }
    viewingPost?.let { post ->
        PostDetailDialog(
            post = post,
            canDelete = postsState.isAdmin,
            onDismiss = { viewingPost = null },
            onDelete = {
                postsViewModel.deletePost(post.id)
                viewingPost = null
            }
        )
    }
}

@Composable
private fun HomeShortcutCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    SurfaceCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}
