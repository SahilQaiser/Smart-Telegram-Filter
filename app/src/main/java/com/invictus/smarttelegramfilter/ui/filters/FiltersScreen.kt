package com.invictus.smarttelegramfilter.ui.filters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilterWithKeywords

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(viewModel: FiltersViewModel, onBack: () -> Unit, onBrowseChannels: () -> Unit) {
    val channels        by viewModel.channels.collectAsStateWithLifecycle()
    val matchCounts     by viewModel.matchCountByChannel.collectAsStateWithLifecycle()
    val isLoading       by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbar  = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filter Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AddChannelRow(
                isLoading = isLoading,
                onAdd = viewModel::addChannelByHandle,
                onBrowse = onBrowseChannels,
            )
            Divider()
            if (channels.isEmpty()) {
                EmptyFilters()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(channels, key = { it.filter.channelId }) { entry ->
                        ChannelCard(
                            entry      = entry,
                            matchCount = matchCounts[entry.filter.channelId] ?: 0,
                            onToggle   = { viewModel.toggleActive(entry.filter.channelId, entry.filter.isActive) },
                            onDelete   = { viewModel.removeChannel(entry.filter) },
                            onSaveKw   = { csv -> viewModel.setKeywordsFromCsv(entry.filter.channelId, csv) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddChannelRow(isLoading: Boolean, onAdd: (String) -> Unit, onBrowse: () -> Unit) {
    var handle by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onBrowse, modifier = Modifier.fillMaxWidth()) {
            Text("Browse subscribed channels")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                modifier = Modifier.weight(1f),
                label = { Text("Or add by handle (@channel)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    if (handle.isNotBlank()) { onAdd(handle); handle = "" }
                }),
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
            } else {
                IconButton(
                    onClick = {
                        focus.clearFocus()
                        if (handle.isNotBlank()) { onAdd(handle); handle = "" }
                    },
                    enabled = handle.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add channel")
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    entry: ChannelFilterWithKeywords,
    matchCount: Int,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSaveKw: (String) -> Unit,
) {
    val filter = entry.filter
    var expanded by remember { mutableStateOf(false) }
    val currentCsv = remember(entry.keywords) {
        entry.keywords.joinToString(", ") { kw ->
            if (kw.isRegex) "r/${kw.pattern}" else kw.pattern
        }
    }
    var kwText by remember(currentCsv) { mutableStateOf(currentCsv) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(filter.channelName, style = MaterialTheme.typography.titleSmall)
                    if (filter.channelHandle.isNotEmpty()) {
                        Text(
                            "@${filter.channelHandle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (matchCount > 0) {
                        Text(
                            "$matchCount match${if (matchCount == 1) "" else "es"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(checked = filter.isActive, onCheckedChange = { onToggle() })
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Keyword chip preview when collapsed
            if (!expanded && entry.keywords.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${entry.keywords.size} keyword(s): $currentCsv",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // Expanded keyword editor
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "Keywords (comma-separated; prefix r/ for regex)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = kwText,
                        onValueChange = { kwText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("bitcoin, r/eth(ereum)?, NFT", fontFamily = FontFamily.Monospace) },
                        minLines = 2,
                        maxLines = 4,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { kwText = currentCsv }) { Text("Reset") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onSaveKw(kwText) }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilters() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No channels tracked yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Browse your subscribed channels or enter a public handle (e.g. @cryptonews) to start filtering",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
