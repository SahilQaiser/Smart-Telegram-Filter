package com.invictus.smarttelegramfilter.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val quietEnabled by viewModel.quietEnabled.collectAsStateWithLifecycle()
    val quietStart   by viewModel.quietStart.collectAsStateWithLifecycle()
    val quietEnd     by viewModel.quietEnd.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Notifications", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quiet hours", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Suppress notifications during selected hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = quietEnabled, onCheckedChange = viewModel::setQuietEnabled)
            }

            AnimatedVisibility(visible = quietEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "From: ${formatHour(quietStart)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = quietStart.toFloat(),
                        onValueChange = { viewModel.setQuietStart(it.toInt()) },
                        valueRange = 0f..23f,
                        steps = 22,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "To: ${formatHour(quietEnd)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = quietEnd.toFloat(),
                        onValueChange = { viewModel.setQuietEnd(it.toInt()) },
                        valueRange = 0f..23f,
                        steps = 22,
                    )

                    Spacer(Modifier.height(4.dp))
                    val rangeLabel = if (quietStart == quietEnd)
                        "No quiet window (start = end)"
                    else if (quietStart < quietEnd)
                        "Quiet ${formatHour(quietStart)} – ${formatHour(quietEnd)}"
                    else
                        "Quiet ${formatHour(quietStart)} – ${formatHour(quietEnd)} (wraps midnight)"
                    Text(
                        rangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatHour(h: Int): String {
    val displayH = if (h % 12 == 0) 12 else h % 12
    val amPm     = if (h < 12) "AM" else "PM"
    return "$displayH:00 $amPm"
}
