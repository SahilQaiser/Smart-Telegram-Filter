package com.invictus.smarttelegramfilter.ui.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import com.invictus.smarttelegramfilter.notification.buildTelegramDeepLink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToFilters: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val messages           by viewModel.messages.collectAsStateWithLifecycle()
    val hasMessages        by viewModel.hasMessages.collectAsStateWithLifecycle()
    val unreadCount        by viewModel.unreadCount.collectAsStateWithLifecycle()
    val channelKeywords    by viewModel.channelKeywords.collectAsStateWithLifecycle()
    val searchQuery        by viewModel.searchQuery.collectAsStateWithLifecycle()
    val archivedMessages   by viewModel.archivedMessages.collectAsStateWithLifecycle()
    val archivedCount      by viewModel.archivedCount.collectAsStateWithLifecycle()
    val availableChannels  by viewModel.availableChannels.collectAsStateWithLifecycle()
    val selectedChannelId  by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val showStarredOnly    by viewModel.showStarredOnly.collectAsStateWithLifecycle()

    var showClearDialog  by remember { mutableStateOf(false) }
    var showArchive      by remember { mutableStateOf(false) }
    val expandedIds      = remember { mutableStateOf(setOf<Long>()) }
    val archiveSheetState = rememberModalBottomSheetState()

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all messages?") },
            text  = { Text("This will permanently delete all matched messages (archive not affected).") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showArchive) {
        ArchiveSheet(
            messages   = archivedMessages,
            sheetState = archiveSheetState,
            onDismiss  = { showArchive = false },
            onUnarchive = viewModel::unarchive,
            onDelete   = viewModel::delete,
            onClearAll = viewModel::clearArchive,
        )
    }

    Scaffold(
        floatingActionButton = {
            BadgedBox(badge = {
                if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
            }) {
                FloatingActionButton(onClick = onNavigateToFilters) {
                    Icon(Icons.Default.FilterList, contentDescription = "Manage filters")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Smart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(" Filter", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = viewModel::markAllRead) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = "Mark all read")
                        }
                    }
                    if (hasMessages) {
                        IconButton(onClick = { viewModel.showStarredOnly.value = !showStarredOnly }) {
                            Icon(
                                if (showStarredOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Starred only",
                                tint = if (showStarredOnly) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { showArchive = true }) {
                        BadgedBox(badge = {
                            if (archivedCount > 0) Badge { Text(archivedCount.toString()) }
                        }) {
                            Icon(Icons.Default.Inbox, contentDescription = "Archive")
                        }
                    }
                    if (hasMessages) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasMessages) {
            EmptyFeed(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("Search messages…") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                if (availableChannels.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedChannelId == null,
                                onClick  = { viewModel.selectChannel(null) },
                                label    = { Text("All") },
                            )
                        }
                        items(availableChannels) { (id, name) ->
                            FilterChip(
                                selected = selectedChannelId == id,
                                onClick  = { viewModel.selectChannel(id) },
                                label    = { Text(name) },
                            )
                        }
                    }
                }

                if (messages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No messages match your current filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val expanded = msg.id in expandedIds.value
                            val dismissState = rememberSwipeToDismissBoxState()

                            LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                    viewModel.archive(msg)
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val isStart = dismissState.dismissDirection ==
                                            SwipeToDismissBoxValue.StartToEnd
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                MaterialTheme.shapes.medium,
                                            ),
                                        contentAlignment = if (isStart) Alignment.CenterStart
                                                           else Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Default.Archive,
                                            contentDescription = "Archive",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                        )
                                    }
                                },
                            ) {
                                MessageCard(
                                    message         = msg,
                                    expanded        = expanded,
                                    channelKeywords = channelKeywords[msg.channelId] ?: emptyList(),
                                    onToggleStar    = { viewModel.toggleStar(msg) },
                                    onClick = {
                                        expandedIds.value = if (expanded)
                                            expandedIds.value - msg.id
                                        else
                                            expandedIds.value + msg.id
                                        if (!msg.isRead) viewModel.markRead(msg.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveSheet(
    messages: List<MatchedMessage>,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onUnarchive: (MatchedMessage) -> Unit,
    onDelete: (MatchedMessage) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear archive?") },
            text  = { Text("Permanently delete all ${messages.size} archived messages.") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false; onDismiss() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Archive (${messages.size})", style = MaterialTheme.typography.titleMedium)
            if (messages.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        HorizontalDivider()
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Archive is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            onDelete(msg)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        MaterialTheme.shapes.medium,
                                    ),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                        },
                    ) {
                        ArchivedMessageRow(msg, onUnarchive = { onUnarchive(msg) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedMessageRow(message: MatchedMessage, onUnarchive: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    message.channelName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    message.textContent,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onUnarchive) {
                Icon(
                    Icons.Default.Unarchive,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
    message: MatchedMessage,
    expanded: Boolean,
    channelKeywords: List<String>,
    onToggleStar: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val unread = !message.isRead
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (unread)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unread) 4.dp else 1.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        if (unread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message.channelName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.7f),
                    )
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star",
                            tint = if (message.isStarred) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = highlightKeyword(message.textContent, message.matchedKeyword),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KeywordChip(message.matchedKeyword)
                    if (expanded) {
                        Row {
                            // Share
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        append(message.channelName)
                                        append("\n\n")
                                        append(message.textContent)
                                        append("\n\n# ")
                                        append(message.matchedKeyword)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            },
                                            "Share message",
                                        )
                                    )
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // Open in Telegram
                            TextButton(
                                onClick = {
                                    val url = buildTelegramDeepLink(
                                        message.channelUsername,
                                        message.channelId,
                                        message.telegramMessageId,
                                    )
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text("Open in Telegram", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (expanded) {
                    val others = channelKeywords.filter {
                        it.lowercase() != message.matchedKeyword.lowercase()
                    }
                    if (others.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Also tracking:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.7f),
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            others.forEach { kw -> KeywordChip(kw, dimmed = true) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordChip(keyword: String, dimmed: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (dimmed)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "# $keyword",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (dimmed) FontWeight.Normal else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .alpha(if (dimmed) 0.6f else 1f)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyFeed(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text("No matched messages yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add channels and keywords via the filter icon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val URL_REGEX = Regex("""https?://\S+""")

private sealed interface Span {
    val start: Int; val end: Int
    data class Url(override val start: Int, override val end: Int, val url: String) : Span
    data class Keyword(override val start: Int, override val end: Int) : Span
}

private fun highlightKeyword(text: String, keyword: String): androidx.compose.ui.text.AnnotatedString {
    val lower = text.lowercase()
    val kw = keyword.lowercase()
    val spans = mutableListOf<Span>()
    URL_REGEX.findAll(text).forEach { spans.add(Span.Url(it.range.first, it.range.last + 1, it.value)) }
    if (kw.isNotEmpty()) {
        var pos = 0
        while (true) {
            val idx = lower.indexOf(kw, pos)
            if (idx == -1) break
            val kwEnd = idx + kw.length
            if (spans.filterIsInstance<Span.Url>().none { idx in it.start until it.end })
                spans.add(Span.Keyword(idx, kwEnd))
            pos = kwEnd
        }
    }
    spans.sortBy { it.start }
    return buildAnnotatedString {
        var cursor = 0
        for (span in spans) {
            if (span.start > cursor) append(text.substring(cursor, span.start))
            when (span) {
                is Span.Url -> withLink(
                    LinkAnnotation.Url(span.url, TextLinkStyles(SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFF1565C0),
                        textDecoration = TextDecoration.Underline,
                    )))
                ) { append(text.substring(span.start, span.end)) }
                is Span.Keyword -> withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = androidx.compose.ui.graphics.Color(0x40FFD700),
                )) { append(text.substring(span.start, span.end)) }
            }
            cursor = span.end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

private fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    return if (now - ts < 86_400_000L) timeFormat.format(Date(ts))
    else dateFormat.format(Date(ts))
}
