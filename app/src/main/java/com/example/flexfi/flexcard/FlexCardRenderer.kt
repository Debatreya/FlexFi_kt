package com.example.flexfi.flexcard

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.example.flexfi.R
import java.io.InputStream
import kotlin.math.min

object FlexCardRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    fun render(context: Context, data: FlexCardData): Bitmap {
        val spec = CardTemplateEngine.spec(data.template)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                HEIGHT.toFloat(),
                spec.backgroundTop,
                spec.backgroundBottom,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        val cardRect = RectF(50f, 240f, WIDTH - 50f, HEIGHT - 300f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.bottom,
                spec.cardOverlayTop,
                spec.cardOverlayBottom,
                Shader.TileMode.CLAMP
            )
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = spec.borderColor
        }

        canvas.drawRoundRect(cardRect, 38f, 38f, cardPaint)
        canvas.drawRoundRect(cardRect, 38f, 38f, borderPaint)

        drawLogo(context, canvas)
        drawHeaderText(canvas, data, spec)
        drawProfile(context.contentResolver, canvas, data.profilePhotoUri, data.initials)
        drawScoreBlock(canvas, data, spec)
        drawInsights(canvas, data, spec)
        drawStatsBar(canvas, data)
        drawTagline(canvas, data, spec, cardRect.bottom)

        return bitmap
    }

    private fun drawLogo(context: Context, canvas: Canvas) {
        val logo = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground) ?: return
        val dest = RectF(56f, 72f, 126f, 142f)
        canvas.drawBitmap(logo, null, dest, null)
    }

    private fun drawHeaderText(canvas: Canvas, data: FlexCardData, spec: CardTemplateSpec) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCE9FF")
            textSize = spec.titleSize
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A8C1E2")
            textSize = spec.bodySize - 6f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        canvas.drawText("YOUR FLEX CARD", 170f, 122f, titlePaint)
        canvas.drawText(data.monthLabel.uppercase(), 170f, 168f, subPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.nameColor
            textSize = spec.titleSize - 2f
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawText(data.userName.uppercase(), 96f, 430f, namePaint)
    }

    private fun drawProfile(contentResolver: ContentResolver, canvas: Canvas, imageUri: Uri?, initials: String) {
        val avatarRect = RectF(760f, 330f, 950f, 520f)
        val avatar = imageUri?.let { decodeBitmap(contentResolver, it) }
        if (avatar != null) {
            canvas.drawBitmap(avatar, null, avatarRect, null)
            return
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4D8E")
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        }

        canvas.drawRoundRect(avatarRect, 28f, 28f, fillPaint)
        val centerX = avatarRect.centerX()
        val centerY = avatarRect.centerY() + 18f
        canvas.drawText(initials.take(2).uppercase(), centerX, centerY, textPaint)
    }

    private fun drawScoreBlock(canvas: Canvas, data: FlexCardData, spec: CardTemplateSpec) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B7CDEE")
            textSize = spec.bodySize - 2f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.scoreColor
            textSize = spec.scoreSize + 48f
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        }

        val (gradeText, gradeColor, gradeSymbol) = gradeMeta(data.score.grade)
        val (trendText, trendColor, trendSymbol) = trendMeta(data.score.trend)

        val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gradeColor
            textSize = spec.bodySize + 6f
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val trendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trendColor
            textSize = spec.bodySize + 6f
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        canvas.drawText("FLEXFI SCORE", 96f, 578f, labelPaint)
        canvas.drawText(data.score.score.toString(), 96f, 708f, scorePaint)
        canvas.drawText("$gradeSymbol $gradeText", 96f, 770f, gradePaint)
        canvas.drawText("$trendSymbol $trendText", 430f, 770f, trendPaint)
    }

    private fun drawInsights(canvas: Canvas, data: FlexCardData, spec: CardTemplateSpec) {
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#142A47")
            alpha = 180
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2F7FF")
            textSize = spec.titleSize - 6f
            isFakeBoldText = true
            typeface = Typeface.create("serif", Typeface.BOLD)
        }
        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F0FF")
            textSize = spec.bodySize - 2f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val improveTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9298")
            textSize = spec.bodySize + 6f
            isFakeBoldText = true
            typeface = Typeface.create("serif", Typeface.BOLD)
        }

        val highlightsPanel = RectF(84f, 820f, 996f, 1198f)
        val improvePanel = RectF(84f, 1216f, 996f, 1442f)

        canvas.drawRoundRect(highlightsPanel, 28f, 28f, panelPaint)
        canvas.drawRoundRect(improvePanel, 28f, 28f, panelPaint)

        canvas.drawText("Highlights", 112f, 886f, titlePaint)

        var y = 946f
        data.highlights.take(3).forEach { line ->
            val bullet = "◆"
            drawWrappedText(
                canvas = canvas,
                text = "$bullet $line",
                x = 112f,
                y = y,
                maxWidth = 860f,
                lineHeight = 48f,
                paint = itemPaint,
                maxLines = 2
            )
            y += 100f
        }

        canvas.drawText("Areas for Improvement", 112f, 1270f, improveTitle)
        drawWrappedText(
            canvas = canvas,
            text = "▾ ${data.improvement}",
            x = 112f,
            y = 1328f,
            maxWidth = 860f,
            lineHeight = 46f,
            paint = itemPaint,
            maxLines = 3
        )
    }

    private fun drawStatsBar(canvas: Canvas, data: FlexCardData) {
        val barRect = RectF(84f, 1462f, 996f, 1556f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                barRect.left,
                barRect.top,
                barRect.right,
                barRect.bottom,
                Color.parseColor("#173E72"),
                Color.parseColor("#1E625C"),
                Shader.TileMode.CLAMP
            )
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            isFakeBoldText = true
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6E7FF")
            textSize = 30f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        canvas.drawRoundRect(barRect, 28f, 28f, fillPaint)
        canvas.drawText(data.monthlySpendText, 118f, 1524f, textPaint)
        canvas.drawText(data.streakText, 620f, 1524f, textPaint)
        canvas.drawText("Spent This Month", 118f, 1552f, subPaint)
        canvas.drawText("Streak", 620f, 1552f, subPaint)
    }

    private fun drawTagline(canvas: Canvas, data: FlexCardData, spec: CardTemplateSpec, cardBottom: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.scoreColor
            textSize = spec.bodySize + 8f
            isFakeBoldText = true
            typeface = Typeface.create("serif", Typeface.ITALIC)
        }

        val topPadding = 78f
        val textStartY = cardBottom + topPadding

        drawWrappedText(
            canvas = canvas,
            text = data.tagline,
            x = 76f,
            y = textStartY,
            maxWidth = 900f,
            lineHeight = 38f,
            paint = paint,
            maxLines = 2
        )
    }

    private fun gradeMeta(grade: String): Triple<String, Int, String> {
        return when (grade.uppercase()) {
            "ELITE" -> Triple("Elite Grade", Color.parseColor("#B98EFF"), "✦")
            "GOLD" -> Triple("Gold Grade", Color.parseColor("#F3D377"), "◆")
            "SILVER" -> Triple("Silver Grade", Color.parseColor("#D4DFEA"), "◈")
            "BRONZE" -> Triple("Bronze Grade", Color.parseColor("#D49A68"), "⬢")
            else -> Triple("Beginner", Color.parseColor("#FF6B6B"), "●")
        }
    }

    private fun trendMeta(trend: String): Triple<String, Int, String> {
        return when (trend.uppercase()) {
            "IMPROVING" -> Triple("Improving", Color.parseColor("#55E39A"), "↗")
            "DECLINING" -> Triple("Declining", Color.parseColor("#FF737D"), "↘")
            else -> Triple("Stable", Color.parseColor("#5CCBFF"), "→")
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint,
        maxLines: Int
    ) {
        if (text.isBlank()) return

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }

            if (lines.size >= maxLines) break
        }

        if (lines.size < maxLines && current.isNotEmpty()) {
            lines.add(current.toString())
        }

        lines.take(maxLines).forEachIndexed { index, line ->
            canvas.drawText(line, x, y + (index * lineHeight), paint)
        }
    }

    private fun decodeBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return runCatching {
            val input: InputStream = contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                val decoded = BitmapFactory.decodeStream(stream) ?: return null
                val side = min(decoded.width, decoded.height)
                Bitmap.createBitmap(
                    decoded,
                    (decoded.width - side) / 2,
                    (decoded.height - side) / 2,
                    side,
                    side
                )
            }
        }.getOrNull()
    }
}
