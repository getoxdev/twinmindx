package com.twinmindx.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinmindx.R
import com.twinmindx.data.db.entity.MeetingStatus
import com.twinmindx.domain.models.Meeting
import com.twinmindx.ui.theme.AccentOrange
import com.twinmindx.ui.theme.CardBlueLight
import com.twinmindx.ui.theme.PrimaryBlue
import com.twinmindx.ui.theme.StatusRecording
import com.twinmindx.ui.theme.TextOnPrimary
import com.twinmindx.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToRecording: (String) -> Unit,
    onNavigateToTranscript: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val meetings by viewModel.meetings.collectAsState()

    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.startNewRecording(onNavigateToRecording)
        } else {
            val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
            if (recordAudioGranted) {
                viewModel.startNewRecording(onNavigateToRecording)
            }
        }
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.startNewRecording(onNavigateToRecording)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 32.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                GreetingSection()
            }
            
            item {
                Illustration()
            }

            item {
                Text(
                    text = "Your Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = PrimaryBlue
                )
            }
            itemsIndexed(meetings) { index, meeting ->
                MeetingCard(
                    index = index,
                    meeting = meeting,
                    onClick = {
                        when (meeting.status) {
                            MeetingStatus.RECORDING, MeetingStatus.PAUSED ->
                                onNavigateToRecording(meeting.id)
                            else ->
                                onNavigateToTranscript(meeting.id)
                        }
                    }
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CaptureNotesButton(
                onClick = { checkAndRequestPermissions() }
            )
        }
    }
}

@Composable
fun GreetingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Hey! Start using twinmindx",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}

@Composable
fun Illustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.raw.illustration),
            contentDescription = "illustration",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun MeetingCard(
    index: Int,
    meeting: Meeting,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Meeting ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatDateTime(meeting.startTimeMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            StatusIndicator(status = meeting.status)
        }
    }
}

@Composable
fun StatusIndicator(status: MeetingStatus) {
    val (color, text) = when (status) {
        MeetingStatus.RECORDING -> StatusRecording to "● Recording"
        MeetingStatus.PAUSED -> AccentOrange to "● Paused"
        MeetingStatus.TRANSCRIBING -> PrimaryBlue to "Processing"
        MeetingStatus.SUMMARIZING -> PrimaryBlue to "Summarizing"
        MeetingStatus.COMPLETED -> Color(0xFF10B981) to "Done"
        MeetingStatus.ERROR -> StatusRecording to "Error"
        MeetingStatus.STOPPED -> TextSecondary to "Stopped"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun CaptureNotesButton(
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
            contentColor = TextOnPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = TextOnPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Capture Notes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatDateTime(timeMs: Long): String {
    val dateFormatter = SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault())
    return dateFormatter.format(Date(timeMs))
}