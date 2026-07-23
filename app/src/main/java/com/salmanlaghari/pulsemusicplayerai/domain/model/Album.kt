package com.salmanlaghari.pulsemusicplayerai.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songsCount: Int,
    val artUri: Uri? = null
)
