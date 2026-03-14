package com.varaha.twinmindx.ui.summary

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varaha.twinmindx.data.db.entity.SummaryStatus
import com.varaha.twinmindx.domain.model.Summary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    meetingId: String,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val summaryFlow = remember(meetingId) { viewModel.getSummaryFlow(meetingId) }
    val summary by summaryFlow.collectAsState()

    LaunchedEffect(meetingId) {
        viewModel.loadSummary(meetingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Summary") })
        }
    ) { padding ->
        when {
            summary == null || summary?.status == SummaryStatus.PENDING -> {
                LoadingContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = "Generating summary..."
                )
            }
            summary?.status == SummaryStatus.ERROR -> {
                ErrorContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = summary?.errorMessage ?: "An error occurred",
                    onRetry = { viewModel.retryGeneration(meetingId) }
                )
            }
            else -> {
                SummaryContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    summary = summary!!
                )
            }
        }
    }
}

@Composable
fun SummaryContent(
    modifier: Modifier = Modifier,
    summary: Summary
) {
    val isStreaming = summary.status == SummaryStatus.STREAMING
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (summary.title.isNotBlank()) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.headlineMedium
            )
        } else if (isStreaming) {
            ShimmerBlock(height = 32)
        }

        SummarySection(
            title = "Summary",
            content = summary.summary,
            isStreaming = isStreaming && summary.summary.isBlank()
        )
        SummarySection(
            title = "Action Items",
            content = summary.actionItems,
            isStreaming = isStreaming && summary.actionItems.isBlank()
        )
        SummarySection(
            title = "Key Points",
            content = summary.keyPoints,
            isStreaming = isStreaming && summary.keyPoints.isBlank()
        )
    }
}

@Composable
fun SummarySection(
    title: String,
    content: String,
    isStreaming: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (isStreaming) {
                ShimmerBlock(height = 80)
            } else if (content.isNotBlank()) {
                Text(content, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ShimmerBlock(height: Int) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(shimmerBrush, MaterialTheme.shapes.small)
    )
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier, message: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShimmerBlock(height = 32)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ErrorContent(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
