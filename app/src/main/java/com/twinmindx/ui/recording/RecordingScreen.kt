package com.twinmindx.ui.recording

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinmindx.service.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    meetingId: String,
    onRecordingStopped: (String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    LaunchedEffect(meetingId) {
        viewModel.bindToService(meetingId)
    }

    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.STOPPED) {
            onRecordingStopped(meetingId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recording") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerDisplay(elapsedSeconds = elapsedSeconds, viewModel = viewModel)

            Spacer(Modifier.height(24.dp))

            StatusIndicator(
                message = statusMessage,
                state = recordingState
            )

            Spacer(Modifier.height(48.dp))

            RecordingControls(
                recordingState = recordingState,
                onPause = { viewModel.pauseRecording() },
                onResume = { viewModel.resumeRecording() },
                onStop = { viewModel.stopRecording() }
            )
        }
    }
}

@Composable
fun TimerDisplay(
    elapsedSeconds: Long,
    viewModel: RecordingViewModel
) {
    Text(
        text = viewModel.formatElapsedTime(elapsedSeconds),
        style = MaterialTheme.typography.displayLarge,
        fontSize = 64.sp
    )
}

@Composable
fun StatusIndicator(message: String, state: RecordingState) {
    val color = when {
        state == RecordingState.PAUSED -> MaterialTheme.colorScheme.tertiary
        message.contains("No audio") -> MaterialTheme.colorScheme.error
        message.contains("Low storage") -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = color
    )
}

@Composable
fun RecordingControls(
    recordingState: RecordingState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val isActive = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED
    val isPaused = recordingState == RecordingState.PAUSED

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            FilledIconButton(
                onClick = if (isPaused) onResume else onPause,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume recording" else "Pause recording",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        RecordingButton(
            isRecording = recordingState == RecordingState.RECORDING,
            onClick = onStop
        )

        if (isActive) {
            Spacer(Modifier.size(64.dp))
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        CircleShape
                    )
            )
        }
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop recording",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
