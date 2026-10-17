package app.vaartalu.player

import app.vaartalu.data.StreamSource

/** Hands out a channel's sources in priority order, once each. */
class SourcePlan(private val sources: List<StreamSource>) {
    private var cursor = 0

    fun next(): StreamSource? = if (cursor < sources.size) sources[cursor++] else null
}
