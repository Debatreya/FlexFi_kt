package com.example.flexfi.flexcard

import android.graphics.Color

data class CardTemplateSpec(
    val nameColor: Int,
    val scoreColor: Int,
    val accentColor: Int,
    val borderColor: Int,
    val backgroundTop: Int,
    val backgroundBottom: Int,
    val cardOverlayTop: Int,
    val cardOverlayBottom: Int,
    val titleSize: Float,
    val scoreSize: Float,
    val bodySize: Float
)

object CardTemplateEngine {

    fun spec(template: CardTemplate): CardTemplateSpec {
        return when (template) {
            CardTemplate.DARK -> CardTemplateSpec(
                nameColor = Color.parseColor("#D7E7FF"),
                scoreColor = Color.parseColor("#FFE08A"),
                accentColor = Color.parseColor("#4AE7C4"),
                borderColor = Color.parseColor("#2FA3FF"),
                backgroundTop = Color.parseColor("#07152B"),
                backgroundBottom = Color.parseColor("#030A16"),
                cardOverlayTop = Color.parseColor("#123D8A"),
                cardOverlayBottom = Color.parseColor("#071B42"),
                titleSize = 56f,
                scoreSize = 86f,
                bodySize = 40f
            )
            CardTemplate.GRADIENT -> CardTemplateSpec(
                nameColor = Color.parseColor("#F3F9FF"),
                scoreColor = Color.parseColor("#FFF0A0"),
                accentColor = Color.parseColor("#7FFFD8"),
                borderColor = Color.parseColor("#5CB3FF"),
                backgroundTop = Color.parseColor("#0D223A"),
                backgroundBottom = Color.parseColor("#143F72"),
                cardOverlayTop = Color.parseColor("#14529F"),
                cardOverlayBottom = Color.parseColor("#0C2A59"),
                titleSize = 58f,
                scoreSize = 90f,
                bodySize = 42f
            )
            CardTemplate.MINIMAL -> CardTemplateSpec(
                nameColor = Color.parseColor("#E8F2FF"),
                scoreColor = Color.parseColor("#FFD774"),
                accentColor = Color.parseColor("#6BE5CA"),
                borderColor = Color.parseColor("#9CB8D8"),
                backgroundTop = Color.parseColor("#122031"),
                backgroundBottom = Color.parseColor("#0E1724"),
                cardOverlayTop = Color.parseColor("#1B2E44"),
                cardOverlayBottom = Color.parseColor("#152739"),
                titleSize = 52f,
                scoreSize = 80f,
                bodySize = 38f
            )
        }
    }
}
