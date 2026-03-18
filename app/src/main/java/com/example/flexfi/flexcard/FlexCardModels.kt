package com.example.flexfi.flexcard

import android.net.Uri

data class FlexScore(
    val score: Int,
    val grade: String,
    val trend: String
)

data class FlexCardLLMResponse(
    val highlights: List<String>,
    val improvement: String,
    val tagline: String
)

enum class CardTemplate {
    DARK,
    GRADIENT,
    MINIMAL
}

data class FlexCardData(
    val userName: String,
    val initials: String,
    val profilePhotoUri: Uri?,
    val score: FlexScore,
    val highlights: List<String>,
    val improvement: String,
    val tagline: String,
    val monthlySpendText: String,
    val streakText: String,
    val template: CardTemplate,
    val monthLabel: String
)
