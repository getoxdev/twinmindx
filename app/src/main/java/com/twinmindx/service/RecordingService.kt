package com.twinmindx.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.twinmindx.MainActivity
import com.twinmindx.data.repository.RecordingRepository
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.util.AudioUtils
import com.twinmindx.util.StorageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var recordingRepository: RecordingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val binder = LocalBinder()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var silenceJob: Job? = null

    private var currentMeetingId: String? = null
    private var chunkIndex = 0
    private var savedChunkCount = 0
    private var elapsedSeconds = 0L
    private var isPausedForCall = false
    private var isPausedForFocus = false

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private lateinit var audioManager: AudioManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var notificationManager: NotificationManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
    private val CHUNK_DURATION_MS = 30_000L
    private val OVERLAP_DURATION_MS = 2_000L
    private val SILENCE_DETECTION_THRESHOLD_MS = 10_000L

    private var overlapBuffer: ShortArray? = null
    private val overlapSamples = (SAMPLE_RATE * OVERLAP_DURATION_MS / 1000).toInt()

    companion object {
        private const val TAG = "RecordingService"
        const val ACTION_STOP_RECORDING = "com.twinmindx.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.twinmindx.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.twinmindx.RESUME_RECORDING"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        registerPhoneStateListener()
        registerAudioDeviceCallback()
        registerHeadsetReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> {
                serviceScope.launch { stopRecording() }
            }
            ACTION_PAUSE_RECORDING -> {
                if (_recordingState.value == RecordingState.RECORDING) {
                    userPauseRecording()
                }
            }
            ACTION_RESUME_RECORDING -> {
                if (_recordingState.value == RecordingState.PAUSED) {
                    userResumeRecording()
                }
            }
            else -> {
                val meetingId = intent?.getStringExtra(EXTRA_MEETING_ID)
                if (meetingId != null && currentMeetingId == null) {
                    startRecording(meetingId)
                }
            }
        }
        return START_STICKY
    }

    fun startRecording(meetingId: String) {
        currentMeetingId = meetingId
        chunkIndex = 0
        savedChunkCount = 0
        elapsedSeconds = 0

        if (!StorageUtils.hasEnoughStorage()) {
            handleLowStorage()
            return
        }

        requestAudioFocus()
        _recordingState.value = RecordingState.RECORDING
        _statusMessage.value = "Recording..."

        startForeground(NOTIFICATION_ID, buildNotification())
        startRecordingLoop()
        startTimer()
    }

    private fun startRecordingLoop() {
        recordingJob = serviceScope.launch {
            overlapBuffer = ShortArray(overlapSamples)
            var overlapBufferIndex: Int

            while (isActive) {
                val meetingId = currentMeetingId ?: break

                if (_recordingState.value == RecordingState.PAUSED) {
                    delay(500)
                    continue
                }

                if (!StorageUtils.hasEnoughStorage()) {
                    handleLowStorage()
                    break
                }

                val file = createChunkFile(meetingId, chunkIndex)
                val currentChunkIndex = chunkIndex

                val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

                if (ContextCompat.checkSelfPermission(this@RecordingService, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    _statusMessage.value = "Error: Recording permission denied"
                    _recordingState.value = RecordingState.ERROR
                    break
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                withContext(Dispatchers.IO) {
                    val outputStream = FileOutputStream(file)
                    AudioUtils.writeWavHeader(outputStream, SAMPLE_RATE, 1, 16)

                    audioRecord?.startRecording()
                    startSilenceDetection()

                    val buffer = ShortArray(bufferSize)
                    val chunkStartMs = System.currentTimeMillis()
                    var totalSamplesWritten = 0

                    if (currentChunkIndex > 0 && overlapBuffer != null) {
                        val overlapBytes = AudioUtils.shortArrayToByteArray(overlapBuffer!!, overlapSamples)
                        outputStream.write(overlapBytes)
                        totalSamplesWritten += overlapSamples
                    }

                    overlapBufferIndex = 0

                    val effectiveDurationMs = if (currentChunkIndex == 0) {
                        CHUNK_DURATION_MS
                    } else {
                        CHUNK_DURATION_MS - OVERLAP_DURATION_MS
                    }

                    while (isActive && _recordingState.value != RecordingState.STOPPED) {
                        if (_recordingState.value == RecordingState.PAUSED) {
                            delay(100)
                            continue
                        }

                        val elapsedMs = System.currentTimeMillis() - chunkStartMs
                        if (elapsedMs >= effectiveDurationMs) break

                        val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                        if (read > 0) {
                            val bytes = ByteArray(read * 2)
                            for (i in 0 until read) {
                                bytes[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                                bytes[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()

                                // Fill overlap buffer with the last 2 seconds of audio
                                overlapBuffer!![overlapBufferIndex % overlapSamples] = buffer[i]
                                overlapBufferIndex++
                            }
                            outputStream.write(bytes)
                            totalSamplesWritten += read

                            val rms = AudioUtils.calculateRms(buffer, read)
                            if (rms > AudioUtils.SILENCE_THRESHOLD) {
                                lastAudioDetectedMs = System.currentTimeMillis()
                                clearSilenceWarning()
                            }
                        }
                    }

                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                    silenceJob?.cancel()

                    outputStream.flush()
                    outputStream.close()
                    AudioUtils.updateWavHeader(file, totalSamplesWritten)

                    if (_recordingState.value == RecordingState.STOPPED && totalSamplesWritten > 0) {
                        val savedChunk = recordingRepository.saveAudioChunk(
                            meetingId = meetingId,
                            chunkIndex = currentChunkIndex,
                            filePath = file.absolutePath,
                            durationMs = CHUNK_DURATION_MS
                        )
                        savedChunkCount++
                    }
                }

                if (_recordingState.value == RecordingState.STOPPED) {
                    break
                }

                val savedChunk = recordingRepository.saveAudioChunk(
                    meetingId = meetingId,
                    chunkIndex = currentChunkIndex,
                    filePath = file.absolutePath,
                    durationMs = CHUNK_DURATION_MS
                )

                savedChunkCount++
                chunkIndex++
            }

            overlapBuffer = null
        }
    }

    private var lastAudioDetectedMs = System.currentTimeMillis()

    private fun clearSilenceWarning() {
        if (_statusMessage.value == "No audio detected - Check microphone") {
            if (_recordingState.value == RecordingState.RECORDING) {
                _statusMessage.value = "Recording..."
            }
            updateNotification()
        }
    }

    private fun startSilenceDetection() {
        silenceJob?.cancel()
        lastAudioDetectedMs = System.currentTimeMillis()
        silenceJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (System.currentTimeMillis() - lastAudioDetectedMs >= SILENCE_DETECTION_THRESHOLD_MS) {
                    _statusMessage.value = "No audio detected - Check microphone"
                    updateNotification()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _recordingState.value != RecordingState.STOPPED) {
                delay(1000)
                if (_recordingState.value == RecordingState.RECORDING) {
                    elapsedSeconds++
                    _elapsedTime.value = elapsedSeconds
                    if (_statusMessage.value == "Recording...") {
                        updateNotification()
                    }
                }
            }
        }
    }

    suspend fun stopRecording() {
        _recordingState.value = RecordingState.STOPPED
        _statusMessage.value = "Stopped"
        recordingJob?.cancel()
        timerJob?.cancel()
        silenceJob?.cancel()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        abandonAudioFocus()

        currentMeetingId?.let { meetingId ->
            withContext(Dispatchers.IO) {
                recordingRepository.finalizeMeeting(meetingId, savedChunkCount)
            }
        }

        if (Build.VERSION.SDK_INT >= 36) {
            // Android 16+ - use DETACH for smoother transitions
            stopForeground(STOP_FOREGROUND_DETACH)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun pauseRecording(reason: String) {
        _recordingState.value = RecordingState.PAUSED
        _statusMessage.value = reason
        updateNotification()
    }

    fun userPauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            _recordingState.value = RecordingState.PAUSED
            _statusMessage.value = "Paused"
            updateNotification()
        }
    }

    fun userResumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED && !isPausedForCall && !isPausedForFocus) {
            _recordingState.value = RecordingState.RECORDING
            _statusMessage.value = "Recording..."
            lastAudioDetectedMs = System.currentTimeMillis()
            updateNotification()
        }
    }

    private fun resumeRecording() {
        isPausedForFocus = false
        isPausedForCall = false
        _recordingState.value = RecordingState.RECORDING
        _statusMessage.value = "Recording..."
        lastAudioDetectedMs = System.currentTimeMillis()
        updateNotification()
    }

    private fun handleLowStorage() {
        _statusMessage.value = "Recording stopped - Low storage"
        _recordingState.value = RecordingState.STOPPED
        updateNotification()
        serviceScope.launch { stopRecording() }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (!isPausedForCall) {
                    isPausedForFocus = true
                    pauseRecording("Paused - Audio focus lost")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPausedForFocus && !isPausedForCall) {
                    resumeRecording()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null

    private fun registerPhoneStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state)
                    }
                }
                telephonyCallback = callback
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "Phone state listener not registered: READ_PHONE_STATE permission denied")
            }
        } else {
            try {
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                }
                phoneStateListener = listener
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "Phone state listener not registered: READ_PHONE_STATE permission denied")
            }
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (_recordingState.value == RecordingState.RECORDING) {
                    isPausedForCall = true
                    pauseRecording("Paused - Phone call")
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isPausedForCall) {
                    isPausedForCall = false
                    if (!isPausedForFocus) {
                        resumeRecording()
                    }
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val hasHeadset = addedDevices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (hasHeadset && _recordingState.value == RecordingState.RECORDING) {
                _statusMessage.value = "Audio source changed - Headset connected"
                updateNotification()
                serviceScope.launch {
                    delay(2000)
                    if (_statusMessage.value == "Audio source changed - Headset connected") {
                        _statusMessage.value = "Recording..."
                        updateNotification()
                    }
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val hadHeadset = removedDevices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (hadHeadset && _recordingState.value == RecordingState.RECORDING) {
                _statusMessage.value = "Audio source changed - Headset disconnected"
                updateNotification()
                serviceScope.launch {
                    delay(2000)
                    if (_statusMessage.value == "Audio source changed - Headset disconnected") {
                        _statusMessage.value = "Recording..."
                        updateNotification()
                    }
                }
            }
        }
    }

    private fun registerAudioDeviceCallback() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (_recordingState.value == RecordingState.RECORDING) {
                    val message =
                        when(state) {
                            0 -> "Audio source changed - Wired headset disconnected"
                            1 -> "Audio source changed - Wired headset connected"
                            else -> "Recording..."
                        }
                    _statusMessage.value = message
                    updateNotification()

                    serviceScope.launch {
                        delay(2000)
                        if (_statusMessage.value == message) {
                            _statusMessage.value = "Recording..."
                            updateNotification()
                        }
                    }
                }
            }
        }
    }

    private fun registerHeadsetReceiver() {
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headsetReceiver, filter)
    }

    private fun createChunkFile(meetingId: String, index: Int): File {
        val dir = File(filesDir, "recordings/$meetingId")
        dir.mkdirs()
        return File(dir, "chunk_${index.toString().padStart(4, '0')}.wav")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recordingChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording status"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP_RECORDING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE_RECORDING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME_RECORDING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_MEETING_ID, currentMeetingId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = _statusMessage.value
        val isPaused = _recordingState.value == RecordingState.PAUSED

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind Recording")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)

        if (isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
        } else if (_recordingState.value == RecordingState.RECORDING) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
        }

        if (_recordingState.value == RecordingState.RECORDING) {
            builder.setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - (elapsedSeconds * 1000))
                .setChronometerCountDown(false)
        }

        return builder.build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_recordingState.value == RecordingState.RECORDING ||
            _recordingState.value == RecordingState.PAUSED) {
            serviceScope.launch { stopRecording() }
        }
        unregisterReceiver(headsetReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
}

enum class RecordingState {
    IDLE, RECORDING, PAUSED, STOPPED, ERROR
}
