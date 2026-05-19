package com.invictus.smarttelegramfilter.engine

import com.invictus.smarttelegramfilter.data.db.entity.Keyword
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe multi-channel matching engine.
 *
 * - Literal keywords  → Aho-Corasick (O(n) scan, rebuilt once per filter change)
 * - Regex keywords    → compiled Regex list (fallback, evaluated in order)
 *
 * [rebuild] is called by TelegramService whenever the active filter set changes.
 * [match] and [isTracking] are called on the IO dispatcher for every incoming message.
 */
@Singleton
class MatchingEngine @Inject constructor() {

    private data class ChannelMatcher(
        val aho: AhoCorasick?,
        val regexes: List<Regex>,
    )

    private val lock = ReentrantReadWriteLock()
    private var matchers: Map<Long, ChannelMatcher> = emptyMap()

    fun rebuild(keywordsByChannel: Map<Long, List<Keyword>>) = lock.write {
        matchers = keywordsByChannel.mapValues { (_, kws) ->
            val literals = kws.filter { !it.isRegex }.map { it.pattern }
            val regexes = kws
                .filter { it.isRegex }
                .mapNotNull { kw ->
                    runCatching { Regex(kw.pattern, RegexOption.IGNORE_CASE) }.getOrNull()
                }
            ChannelMatcher(
                aho = if (literals.isNotEmpty()) AhoCorasick.build(literals) else null,
                regexes = regexes,
            )
        }
    }

    /**
     * Returns the first matched keyword/fragment for [channelId] in [text],
     * or null if no match (including when the channel is not tracked).
     */
    fun match(channelId: Long, text: String): String? = lock.read {
        val m = matchers[channelId] ?: return null
        m.aho?.firstMatch(text)?.let { return it }
        m.regexes.forEach { r -> r.find(text)?.let { return it.value } }
        return null
    }

    fun isTracking(channelId: Long): Boolean = lock.read { channelId in matchers }
}
