package com.invictus.smarttelegramfilter.telegram

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Singleton wrapper around the native TDLib [Client].
 *
 * Setup (one-time, per project):
 *   1. Build TDLib for Android from https://github.com/tdlib/td
 *      or download a prebuilt distribution.
 *   2. Drop `tdlib.jar` into `app/libs/`.
 *   3. Drop `libtdjni.so` into `app/src/main/jniLibs/<ABI>/`
 *      (arm64-v8a, armeabi-v7a, x86, x86_64 as needed).
 *   4. Set TELEGRAM_API_ID and TELEGRAM_API_HASH in `local.properties`.
 */
@Singleton
class TdlibClient @Inject constructor() {

    companion object {
        init {
            System.loadLibrary("tdjni")
        }
    }

    private var client: Client? = null

    private val _updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 512)
    private val _chatFolderIds = MutableStateFlow<List<Int>>(emptyList())

    /** Hot flow of all TDLib updates — collect in [TelegramService]. */
    val updates: SharedFlow<TdApi.Object> = _updates.asSharedFlow()

    /** IDs of the user's chat folders, populated from UpdateChatFolders. */
    val chatFolderIds: StateFlow<List<Int>> = _chatFolderIds.asStateFlow()

    val isInitialized: Boolean get() = client != null

    /** Called once by [TelegramService.onCreate]. */
    fun initialize() {
        check(client == null) { "TdlibClient already initialized" }
        client = Client.create(
            /* updateHandler           */ { obj ->
                if (obj is TdApi.UpdateChatFolders) {
                    _chatFolderIds.value = obj.chatFolders.map { it.id }
                }
                _updates.tryEmit(obj)
            },
            /* updateExceptionHandler  */ null,
            /* defaultExceptionHandler */ null,
        )
    }

    /**
     * Suspending TDLib call — resumes on the TDLib worker thread and is
     * safe to call from any coroutine context.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : TdApi.Object> send(function: TdApi.Function<T>): T =
        suspendCoroutine { cont ->
            val c = requireNotNull(client) { "TdlibClient not initialized — start TelegramService first" }
            c.send(function) { result ->
                if (result is TdApi.Error) {
                    cont.resumeWithException(TdlibException(result.code, result.message))
                } else {
                    cont.resume(result as T)
                }
            }
        }

    fun close() {
        client?.send(TdApi.Close()) { }
        client = null
    }
}

class TdlibException(val code: Int, message: String) : Exception("TDLib [$code]: $message")
