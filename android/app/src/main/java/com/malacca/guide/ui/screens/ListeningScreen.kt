package com.malacca.guide.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.malacca.guide.ui.navigation.ROUTE_HOME
import com.malacca.guide.ui.navigation.ROUTE_LOADING
import com.malacca.guide.ui.theme.BackgroundDark
import com.malacca.guide.ui.theme.ErrorRed
import com.malacca.guide.ui.theme.MalaccaTeal
import com.malacca.guide.ui.theme.TextPrimary
import com.malacca.guide.ui.theme.TextSecondary
import com.malacca.guide.ui.viewmodel.GuideViewModel

@Composable
fun ListeningScreen(navController: NavController, viewModel: GuideViewModel) {
    val context = LocalContext.current
    val localeTag = when (viewModel.selectedLanguage) {
        "ZH" -> "zh-CN"
        "MS" -> "ms-MY"
        else -> "en-US"
    }
    val listeningText = when (viewModel.selectedLanguage) {
        "ZH" -> "正在聆听..."
        "MS" -> "Mendengar..."
        else -> "Listening..."
    }
    val speakNowText = when (viewModel.selectedLanguage) {
        "ZH" -> "请说话..."
        "MS" -> "Sila bercakap..."
        else -> "Speak now..."
    }
    val stopText = when (viewModel.selectedLanguage) {
        "ZH" -> "停止"
        "MS" -> "BERHENTI"
        else -> "STOP"
    }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val sttIntent = remember(localeTag) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recognizer.startListening(sttIntent)
        else navController.popBackStack(ROUTE_HOME, inclusive = false)
    }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(bundle: Bundle) {
                val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                viewModel.updateTranscript(text)
                navController.navigate(ROUTE_LOADING)
            }
            override fun onError(error: Int) {
                navController.popBackStack(ROUTE_HOME, inclusive = false)
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer.destroy() }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            recognizer.startListening(sttIntent)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = listeningText,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val base = size.minDimension / 6
                drawCircle(
                    color = MalaccaTeal.copy(alpha = alpha * 0.25f),
                    radius = base * scale * 3f,
                    center = center
                )
                drawCircle(
                    color = MalaccaTeal.copy(alpha = alpha * 0.45f),
                    radius = base * scale * 2f,
                    center = center
                )
                drawCircle(
                    color = MalaccaTeal,
                    radius = base,
                    center = center
                )
            }

            Text(
                text = speakNowText,
                color = TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { navController.popBackStack(ROUTE_HOME, inclusive = false) },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text(text = stopText, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
