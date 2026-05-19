package com.invictus.smarttelegramfilter.engine

/**
 * Immutable Aho-Corasick automaton built from a list of literal patterns.
 *
 * Complexity:
 *   build  — O(sum of pattern lengths)
 *   search — O(text length)  regardless of pattern count
 *
 * Unicode-safe: uses Char (UTF-16 code unit) as the alphabet key in HashMaps,
 * so no lossy truncation occurs for non-ASCII Telegram text.
 */
class AhoCorasick private constructor(
    private val children: Array<HashMap<Char, Int>>,
    private val fail: IntArray,
    private val output: Array<List<Int>>,
    private val patterns: List<String>,
) {
    companion object {
        fun build(patterns: List<String>): AhoCorasick {
            if (patterns.isEmpty()) {
                return AhoCorasick(emptyArray(), IntArray(0), emptyArray(), emptyList())
            }

            val ch = ArrayList<HashMap<Char, Int>>()
            val out = ArrayList<MutableList<Int>>()

            fun newNode(): Int {
                ch.add(HashMap(4))
                out.add(mutableListOf())
                return ch.lastIndex
            }
            newNode() // root = 0

            // Build trie
            patterns.forEachIndexed { idx, raw ->
                var cur = 0
                for (c in raw.lowercase()) {
                    cur = ch[cur].getOrPut(c) { newNode() }
                }
                out[cur].add(idx)
            }

            val n = ch.size
            val fail = IntArray(n)
            val queue = ArrayDeque<Int>()

            // Root's direct children: fail → root
            ch[0].values.forEach { child ->
                fail[child] = 0
                queue.add(child)
            }

            while (queue.isNotEmpty()) {
                val u = queue.removeFirst()
                // Propagate suffix outputs (dictionary links)
                out[u].addAll(out[fail[u]])

                ch[u].forEach { (c, v) ->
                    // Walk failure chain from u's parent to find suffix match for c
                    var f = fail[u]
                    while (f != 0 && !ch[f].containsKey(c)) f = fail[f]
                    fail[v] = ch[f][c]?.takeIf { it != v } ?: 0
                    queue.add(v)
                }
            }

            return AhoCorasick(
                children = ch.map { it }.toTypedArray(),
                fail = fail,
                output = out.map { it.toList() }.toTypedArray(),
                patterns = patterns,
            )
        }
    }

    /** Returns the first matched pattern, or null if text contains no pattern. O(|text|). */
    fun firstMatch(text: String): String? {
        if (children.isEmpty()) return null
        var cur = 0
        for (c in text.lowercase()) {
            while (cur != 0 && !children[cur].containsKey(c)) cur = fail[cur]
            cur = children[cur][c] ?: 0
            if (output[cur].isNotEmpty()) return patterns[output[cur].first()]
        }
        return null
    }
}
