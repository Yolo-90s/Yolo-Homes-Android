package com.example.yolo_homes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.core.PostTypes
import com.example.yolo_homes.data.model.Post
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.ChartTeal
import com.example.yolo_homes.ui.theme.LightPrimary
import com.example.yolo_homes.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Locale

private fun colorFor(type: String): Color = when (type) {
    PostTypes.NEWS -> ChartTeal
    PostTypes.EVENT -> Warning
    else -> LightPrimary
}

@Composable
fun PostTypeBadge(type: String, modifier: Modifier = Modifier) {
    val label = Post(type = type).typeLabel
    val color = colorFor(type)
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PostCard(post: Post, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.clickable(onClick = onClick)) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostTypeBadge(post.type)
                if (post.pinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Text(
                post.title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 10.dp)
            )
            if (post.body.isNotBlank()) {
                Text(
                    post.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(Modifier.padding(top = 8.dp)) {
                val meta = buildString {
                    append(Formatters.shortDate(post.createdAtMillis))
                    if (post.type == PostTypes.EVENT && post.eventDateMillis != null) {
                        append("  •  Event: ")
                        append(Formatters.shortDate(post.eventDateMillis))
                    }
                    if (post.createdByName.isNotBlank()) {
                        append("  •  ")
                        append(post.createdByName)
                    }
                }
                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Full post view + admin delete, opened by tapping a card. */
@Composable
fun PostDetailDialog(post: Post, canDelete: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                PostTypeBadge(post.type)
                Text(post.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
        },
        text = {
            Column {
                Text(post.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    buildString {
                        append("Posted ")
                        append(Formatters.shortDate(post.createdAtMillis))
                        if (post.createdByName.isNotBlank()) append(" by ${post.createdByName}")
                        if (post.type == PostTypes.EVENT && post.eventDateMillis != null) {
                            append(" • Event: ${Formatters.shortDate(post.eventDateMillis)}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = if (canDelete) {
            { TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
        } else null
    )
}

/** Admin-only "New Post" composer — title/body/type/optional event date/pinned. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostSheet(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String, type: String, eventDateMillis: Long?, pinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PostTypes.NOTICE) }
    var eventDateText by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("New Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.yolo_homes.ui.components.LabeledDropdown(
                    label = "Type",
                    options = PostTypes.ALL,
                    selected = type,
                    optionLabel = { it.replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { type = it }
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Details") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == PostTypes.EVENT) {
                    OutlinedTextField(
                        value = eventDateText,
                        onValueChange = { eventDateText = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("Event date") },
                        placeholder = { Text("yyyy-MM-dd, e.g. 2026-10-05") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pin to top", modifier = Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val eventMillis = if (type == PostTypes.EVENT) parseEventDate(eventDateText) else null
                    onSave(title.trim(), body.trim(), type, eventMillis, pinned)
                },
                enabled = !saving && title.isNotBlank() && body.isNotBlank()
            ) { Text(if (saving) "Posting…" else "Post") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}

private fun parseEventDate(text: String): Long? = try {
    if (text.isBlank()) null else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(text)?.time
} catch (e: Exception) {
    null
}
