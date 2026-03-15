package com.twinmindx.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinmindx.data.local.entity.SummaryStatus
import com.twinmindx.domain.models.Summary
import com.twinmindx.ui.theme.PrimaryBlue
import com.twinmindx.ui.theme.StatusRecording
import com.twinmindx.ui.theme.TextSecondary

@Composable
fun SummaryScreen(
    onBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp)
    ) {
        when (summary?.status) {
            SummaryStatus.ERROR -> {
                ErrorContent(
                    errorMessage = summary?.errorMessage ?: "Failed to generate summary.",
                    onRetry = { viewModel.retry() },
                    onBack = onBack
                )
            }

            SummaryStatus.COMPLETED -> {
                SummaryContent(
                    summary = summary,
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            else -> {
                LoadingContent(
                    streamingText = summary?.streamingText,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun SummaryContent(
    summary: Summary?,
    viewModel: SummaryViewModel,
    onBack: () -> Unit
) {
    val actionItems = summary?.actionItems
    val keyPoints = summary?.keyPoints

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryTopBar(
            title = summary?.title ?: "Notes Summary",
            subtitle = if (summary?.createdAtMs != null) formatSubtitle(summary.createdAtMs) else "",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!summary?.summary.isNullOrBlank()) {
                item {
                    SummarySection(
                        title = "Summary",
                        canRefresh = true,
                        onRefresh = { viewModel.retry() }
                    ) {
                        val points = summary.summary.split("\n").filter { it.isNotBlank() }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            points.forEach { point ->
                                BulletPoint(text = point.trim().removePrefix("•").trim())
                            }
                        }
                    }
                }
            }

            if (keyPoints?.isNotEmpty() == true) {
                item {
                    SummarySection(title = "Key Points") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            keyPoints.forEach { point ->
                                BulletPoint(text = point)
                            }
                        }
                    }
                }
            }

            if (actionItems?.isNotEmpty() == true) {
                item {
                    SummarySection(title = "Action Items") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            actionItems.forEach { item ->
                                ActionItemRow(text = item)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SummaryTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                ),
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SummarySection(
    title: String,
    canRefresh: Boolean = false,
    onRefresh: () -> Unit = {},
    actionButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canRefresh) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                actionButton?.invoke()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ActionItemRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                .padding(2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun LoadingContent(
    streamingText: String?,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
        }

        if (streamingText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PrimaryBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating summary...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Generating summary...",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = streamingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusRecording,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRecording.copy(alpha = 0.1f),
                        contentColor = StatusRecording
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

private fun formatSubtitle(createdAtMs: Long): String {
    val dateFormat = java.text.SimpleDateFormat("MMM dd • hh:mm a", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(createdAtMs))
}
