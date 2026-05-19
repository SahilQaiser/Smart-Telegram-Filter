package com.invictus.smarttelegramfilter.telegram

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.invictus.smarttelegramfilter.BuildConfig
import com.invictus.smarttelegramfilter.data.db.AppDatabase
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import com.invictus.smarttelegramfilter.engine.MatchingEngine
import com.invictus.smarttelegramfilter.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class TelegramService : Service() {

    @Inject lateinit var tdlib: TdlibClient
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var matchingEngine: MatchingEngine
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Channel metadata cache for fast name lookup without a DB hit per message. */
    private val activeFilters = ConcurrentHashMap<Long, ChannelFilter>()

    // ── Static auth state ─────────────────────────────────────────────────────

    companion object {
        private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)

        /** Observed by AuthViewModel and MainActivity to drive the login flow. */
        val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

        fun start(ctx: Context) =
            ctx.startForegroundService(Intent(ctx, TelegramService::class.java))

        fun stop(ctx: Context) =
            ctx.stopService(Intent(ctx, TelegramService::class.java))
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            notificationHelper.buildServiceNotification(),
        )
        if (!tdlib.isInitialized) tdlib.initialize()
        scope.launch { watchFilterChanges() }
        scope.launch { processTdlibUpdates() }
    }

    override fun onDestroy() {
        scope.cancel()
        tdlib.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Filter cache ──────────────────────────────────────────────────────────

    private suspend fun watchFilterChanges() {
        db.channelFilterDao().observeActiveWithKeywords().collectLatest { list ->
            activeFilters.clear()
            list.forEach { activeFilters[it.filter.channelId] = it.filter }
            val byChannel = list.associate { it.filter.channelId to it.keywords }
            matchingEngine.rebuild(byChannel)
        }
    }

    // ── TDLib update dispatcher ───────────────────────────────────────────────

    private suspend fun processTdlibUpdates() {
        tdlib.updates.collect { obj ->
            when (obj) {
                is TdApi.UpdateAuthorizationState ->
                    handleAuth(obj.authorizationState)
                is TdApi.UpdateNewMessage ->
                    // Launch independently so one slow message never blocks the collector
                    scope.launch { handleNewMessage(obj.message) }
            }
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    private suspend fun handleAuth(state: TdApi.AuthorizationState) {
        _authState.value = state
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> applyTdlibParameters()
            is TdApi.AuthorizationStateClosed -> stopSelf()
            is TdApi.AuthorizationStateWaitPhoneNumber,
            is TdApi.AuthorizationStateWaitCode,
            is TdApi.AuthorizationStateWaitPassword -> notificationHelper.notifyAuthRequired()
            else -> Unit
        }
    }

    private suspend fun applyTdlibParameters() {
        tdlib.send(TdApi.SetTdlibParameters().apply {
            apiId = BuildConfig.TELEGRAM_API_ID
            apiHash = BuildConfig.TELEGRAM_API_HASH
            databaseDirectory = "${filesDir.absolutePath}/tdlib"
            filesDirectory = "${filesDir.absolutePath}/tdlib_files"
            useMessageDatabase = true
            useSecretChats = false
            systemLanguageCode = "en"
            deviceModel = Build.MODEL
            applicationVersion = BuildConfig.VERSION_NAME
        })
    }

    // ── Message pipeline ──────────────────────────────────────────────────────

    private suspend fun handleNewMessage(msg: TdApi.Message) {
        val channelId = msg.chatId
        if (!matchingEngine.isTracking(channelId)) return

        val text = msg.extractText() ?: return
        val keyword = matchingEngine.match(channelId, text) ?: return

        val filter = activeFilters[channelId] ?: return
        val sender = msg.resolveSenderName()

        val matched = MatchedMessage(
            telegramMessageId = msg.id,
            channelId = channelId,
            channelName = filter.channelName,
            channelUsername = filter.channelHandle,
            senderName = sender,
            textContent = text,
            matchedKeyword = keyword,
            timestamp = msg.date.toLong() * 1_000L,
        )

        val rowId = db.matchedMessageDao().insert(matched)
        // rowId == -1 means the (channelId, messageId) pair was already stored — skip notify.
        if (rowId > 0) notificationHelper.notifyMatchedMessage(matched.copy(id = rowId))
    }

    private fun TdApi.Message.extractText(): String? = when (val c = content) {
        is TdApi.MessageText     -> c.text.text
        is TdApi.MessagePhoto    -> c.caption?.text
        is TdApi.MessageVideo    -> c.caption?.text
        is TdApi.MessageDocument -> c.caption?.text
        else                     -> null
    }

    private suspend fun TdApi.Message.resolveSenderName(): String =
        when (val s = senderId) {
            is TdApi.MessageSenderUser -> runCatching {
                val u = tdlib.send(TdApi.GetUser(s.userId))
                "${u.firstName} ${u.lastName}".trim().ifEmpty { "User ${s.userId}" }
            }.getOrDefault("Unknown")

            is TdApi.MessageSenderChat -> runCatching {
                tdlib.send(TdApi.GetChat(s.chatId)).title
            }.getOrDefault("Unknown")

            else -> "Unknown"
        }

    // ── Public API for UI auth flow ───────────────────────────────────────────

    suspend fun submitPhone(phone: String) =
        tdlib.send(TdApi.SetAuthenticationPhoneNumber(phone, null))

    suspend fun submitCode(code: String) =
        tdlib.send(TdApi.CheckAuthenticationCode(code))

    suspend fun submitPassword(password: String) =
        tdlib.send(TdApi.CheckAuthenticationPassword(password))

    suspend fun resolveChannelHandle(handle: String): TdApi.Chat =
        tdlib.send(TdApi.SearchPublicChat(handle.removePrefix("@").trim()))

    suspend fun logOut() =
        tdlib.send(TdApi.LogOut())
}
