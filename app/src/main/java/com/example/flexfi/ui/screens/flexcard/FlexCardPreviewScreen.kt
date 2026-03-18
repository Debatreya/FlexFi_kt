package com.example.flexfi.ui.screens.flexcard

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.flexfi.flexcard.CardTemplate
import com.example.flexfi.flexcard.FlexCardRenderer
import com.example.flexfi.flexcard.FlexCardShareUtil
import com.example.flexfi.ui.components.FlexFiOutlinedButton
import com.example.flexfi.ui.components.FlexFiPrimaryButton
import com.example.flexfi.ui.components.FlexFiTopBar
import com.example.flexfi.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FlexCardPreviewScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val data by viewModel.flexCardData.collectAsState()
    val loading by viewModel.flexCardLoading.collectAsState()
    val error by viewModel.flexCardError.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()

    LaunchedEffect(Unit) {
        if (data == null) {
            viewModel.generateFlexCard()
        }
    }

    val renderedBitmap by produceState<Bitmap?>(initialValue = null, data, selectedTemplate) {
        value = null
        val renderData = data?.copy(template = selectedTemplate)
        if (renderData != null) {
            value = withContext(Dispatchers.Default) {
                FlexCardRenderer.render(context, renderData)
            }
        }
    }

    Scaffold(
        topBar = {
            FlexFiTopBar(
                title = "Your Flex Card",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateChip(
                    label = "Dark",
                    selected = selectedTemplate == CardTemplate.DARK,
                    onClick = { viewModel.selectTemplate(CardTemplate.DARK) }
                )
                TemplateChip(
                    label = "Gradient",
                    selected = selectedTemplate == CardTemplate.GRADIENT,
                    onClick = { viewModel.selectTemplate(CardTemplate.GRADIENT) }
                )
                TemplateChip(
                    label = "Minimal",
                    selected = selectedTemplate == CardTemplate.MINIMAL,
                    onClick = { viewModel.selectTemplate(CardTemplate.MINIMAL) }
                )
            }

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                Text("Building your Flex Card…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (renderedBitmap != null) {
                Image(
                    bitmap = renderedBitmap!!.asImageBitmap(),
                    contentDescription = "Flex Card Preview",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    FlexFiOutlinedButton(
                        text = "Regenerate",
                        onClick = { viewModel.generateFlexCard(force = true) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    FlexFiPrimaryButton(
                        text = "Share",
                        onClick = {
                            val file = FlexCardShareUtil.saveToCache(context, renderedBitmap!!)
                            val intent = FlexCardShareUtil.createShareIntent(context, file)
                            context.startActivity(Intent.createChooser(intent, "Share Flex Card"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TemplateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
