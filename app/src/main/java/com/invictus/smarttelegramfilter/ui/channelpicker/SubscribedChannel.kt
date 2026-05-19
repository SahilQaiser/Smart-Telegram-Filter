package com.invictus.smarttelegramfilter.ui.channelpicker

data class SubscribedChannel(
    val id: Long,
    val title: String,
    val username: String,
    val memberCount: Int,
    val isAlreadyTracked: Boolean,
)
