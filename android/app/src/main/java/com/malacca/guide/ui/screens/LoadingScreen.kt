package com.malacca.guide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.malacca.guide.ui.navigation.ROUTE_HOME
import com.malacca.guide.ui.navigation.ROUTE_RESULT
import com.malacca.guide.ui.theme.BackgroundDark
import com.malacca.guide.ui.theme.MalaccaTeal
import com.malacca.guide.ui.theme.TextPrimary
import com.malacca.guide.ui.theme.TextSecondary
import com.malacca.guide.ui.viewmodel.AppMode
import com.malacca.guide.ui.viewmodel.GuideViewModel
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(navController: NavController, viewModel: GuideViewModel) {
    val isRestaurant = viewModel.appMode == AppMode.RESTAURANT

    val messages = when {
        isRestaurant -> when (viewModel.selectedLanguage) {
            "ZH" -> listOf("正在识别餐厅...", "正在获取评分...", "快好了...")
            "MS" -> listOf("Mengenal pasti restoran...", "Mendapatkan penilaian...", "Sebentar lagi...")
            else -> listOf("Identifying restaurant...", "Fetching ratings...", "Almost there...")
        }
        viewModel.isFollowUp -> when (viewModel.selectedLanguage) {
            "ZH" -> listOf("正在搜索详情...", "查找相关信息...", "快好了...")
            "MS" -> listOf("Mencari maklumat...", "Sedang mencari...", "Sebentar lagi...")
            else -> listOf("Searching for details...", "Looking up information...", "Almost there...")
        }
        else -> when (viewModel.selectedLanguage) {
            "ZH" -> listOf("正在识别地标...", "正在分析照片...", "正在咨询AI导览...", "正在搜索详情...", "快好了...")
            "MS" -> listOf("Mengenal pasti mercu tanda...", "Menganalisis foto anda...", "Merujuk panduan AI...", "Mencari maklumat...", "Sebentar lagi...")
            else -> listOf("Identifying landmark...", "Analyzing your photo...", "Consulting AI guide...", "Searching for details...", "Almost there...")
        }
    }

    val thinkingText = when (viewModel.selectedLanguage) {
        "ZH" -> "HeyCyan正在思考..."
        "MS" -> "HeyCyan sedang berfikir..."
        else -> "HeyCyan is thinking..."
    }

    var messageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    // Kick off the backend call once when screen appears
    LaunchedEffect(Unit) {
        if (isRestaurant) viewModel.analyzeRestaurant()
        else viewModel.analyze()
    }

    // Navigate when result or error arrives
    LaunchedEffect(
        viewModel.analyzeResult, viewModel.analyzeError,
        viewModel.restaurantResult, viewModel.restaurantError
    ) {
        when {
            viewModel.analyzeResult != null -> navController.navigate(ROUTE_RESULT)
            viewModel.restaurantResult != null -> navController.navigate(ROUTE_RESULT)
            viewModel.analyzeError != null -> navController.popBackStack(ROUTE_HOME, inclusive = false)
            viewModel.restaurantError != null -> navController.popBackStack(ROUTE_HOME, inclusive = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MalaccaTeal,
                strokeWidth = 5.dp
            )
            Text(
                text = messages[messageIndex],
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = thinkingText,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
