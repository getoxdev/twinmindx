package com.twinmindx.ui.transcript

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinmindx.data.local.entity.MeetingStatus
import com.twinmindx.domain.models.TranscriptChunk
import com.twinmindx.ui.theme.CardBlueLight
import com.twinmindx.ui.theme.PrimaryBlue
import com.twinmindx.ui.theme.StatusRecording
import com.twinmindx.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptScreen(
    onBack: () -> Unit,
    onViewSummary: () -> Unit,
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    val meeting by viewModel.meeting.collectAsState()
    val chunks by viewModel.transcriptChunks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val isTranscribing = meeting?.status == MeetingStatus.TRANSCRIBING
    val hasError = meeting?.status == MeetingStatus.ERROR
    val canViewSummary = meeting?.status == MeetingStatus.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 32.dp)
    ) {
        TranscriptTopBar(
            title = "Transcript",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isTranscribing -> {
                LoadingContent(message = "Transcribing audio...")
            }
            hasError -> {
                ErrorContent(
                    isRetrying = uiState.isRetrying,
                    onRetry = { viewModel.retryAllChunks() }
                )
            }
            chunks.isEmpty() -> {
                EmptyTranscriptContent()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chunks, key = { it.id }) { chunk ->
                        TranscriptChunkItem(chunk = chunk)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (canViewSummary && chunks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                GetSummaryButton(onClick = onViewSummary)
            }
        }
    }
}

@Composable
fun TranscriptTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back",
                color = PrimaryBlue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
fun TranscriptChunkItem(
    chunk: TranscriptChunk
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timestamp
        Text(
            text = formatTimestamp(chunk.createdAtMs),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Transcript text card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = CardBlueLight.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = chunk.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun LoadingContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PrimaryBlue,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ErrorContent(
    isRetrying: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = StatusRecording
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Some chunks failed to transcribe.",
                style = MaterialTheme.typography.bodyLarge,
                color = StatusRecording
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                enabled = !isRetrying,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRecording.copy(alpha = 0.1f),
                    contentColor = StatusRecording
                )
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = StatusRecording
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry All", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun EmptyTranscriptContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Waiting for transcript...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun GetSummaryButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Get Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTimestamp(timeMs: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(timeMs)).lowercase()
}
