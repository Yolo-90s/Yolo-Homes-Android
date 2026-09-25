package com.example.yolo_homes.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.yolo_homes.core.PdfExporter
import java.io.File

/**
 * The two PDF actions every report/receipt/bill screen offers, with
 * identical labels/icons everywhere so the pattern only needs learning
 * once: **Save to Downloads** (a real file in the device's Downloads
 * folder — [PdfExporter.saveToDownloads]) and **Share** (the existing
 * share-sheet flow — [PdfExporter.share]). [buildFile] is called fresh
 * for each tap so a screen whose data can change (e.g. a report's
 * selected month) always exports what's currently on screen.
 */
@Composable
fun PdfActionsRow(buildFile: () -> File, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val context = LocalContext.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = {
                val file = buildFile()
                val saved = PdfExporter.saveToDownloads(context, file)
                Toast.makeText(
                    context,
                    if (saved != null) "Saved to Downloads" else "Couldn't save directly — use Share instead",
                    Toast.LENGTH_SHORT
                ).show()
            },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Text(" Save to Downloads")
        }
        OutlinedButton(
            onClick = { PdfExporter.share(context, buildFile(), "Share PDF") },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null)
            Text(" Share")
        }
    }
}
