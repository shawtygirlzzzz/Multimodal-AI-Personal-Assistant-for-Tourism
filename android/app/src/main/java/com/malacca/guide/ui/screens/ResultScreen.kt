package com.malacca.guide.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.malacca.guide.api.models.PlaceAlternative
import com.malacca.guide.ui.navigation.ROUTE_HOME
import com.malacca.guide.ui.navigation.ROUTE_LISTENING
import com.malacca.guide.ui.theme.BackgroundDark
import com.malacca.guide.ui.theme.ErrorRed
import com.malacca.guide.ui.theme.MalaccaGold
import com.malacca.guide.ui.theme.MalaccaTeal
import com.malacca.guide.ui.theme.SuccessGreen
import com.malacca.guide.ui.theme.SurfaceDark
import com.malacca.guide.ui.theme.TextPrimary
import com.malacca.guide.ui.theme.TextSecondary
import com.malacca.guide.ui.theme.WarningAmber
import com.malacca.guide.ui.viewmodel.AppMode
import com.malacca.guide.ui.viewmodel.GuideViewModel
import com.malacca.guide.voice.TtsManager

@Composable
fun ResultScreen(navController: NavController, viewModel: GuideViewModel, ttsManager: TtsManager) {

    DisposableEffect(Unit) {
        onDispose { ttsManager.stop() }
    }

    if (viewModel.appMode == AppMode.RESTAURANT) {
        RestaurantResultScreen(navController, viewModel, ttsManager)
    } else {
        LandmarkResultScreen(navController, viewModel, ttsManager)
    }
}

// ── Landmark variant (existing flow) ────────────────────────────────────────

@Composable
private fun LandmarkResultScreen(
    navController: NavController,
    viewModel: GuideViewModel,
    ttsManager: TtsManager
) {
    val result = viewModel.analyzeResult
    val bitmap = viewModel.capturedBitmap
    val landmarkName = result?.landmarkName ?: "Unknown Landmark"
    val responseText = result?.response ?: result?.message ?: "No response received."
    val confidence = result?.confidence?.lowercase() ?: "unknown"

    LaunchedEffect(responseText) {
        ttsManager.speak(responseText, viewModel.selectedLanguage)
    }

    val (badgeColor, badgeLabel) = when (confidence) {
        "high"   -> SuccessGreen to when (viewModel.selectedLanguage) { "ZH" -> "高"; "MS" -> "Tinggi"; else -> "High" }
        "medium" -> WarningAmber to when (viewModel.selectedLanguage) { "ZH" -> "中"; "MS" -> "Sederhana"; else -> "Medium" }
        "low"    -> ErrorRed to when (viewModel.selectedLanguage) { "ZH" -> "低"; "MS" -> "Rendah"; else -> "Low" }
        else     -> TextSecondary to when (viewModel.selectedLanguage) { "ZH" -> "不明"; "MS" -> "Tidak diketahui"; else -> "Unknown" }
    }

    val backText    = when (viewModel.selectedLanguage) { "ZH" -> "← 返回"; "MS" -> "← Kembali"; else -> "← Back" }
    val replayText  = when (viewModel.selectedLanguage) { "ZH" -> "重播"; "MS" -> "Main semula"; else -> "Replay" }
    val followText  = when (viewModel.selectedLanguage) { "ZH" -> "继续提问"; "MS" -> "Tanya lanjut"; else -> "Ask follow-up" }
    val anotherText = when (viewModel.selectedLanguage) { "ZH" -> "询问其他地标"; "MS" -> "Tanya mercu tanda lain"; else -> "Ask another landmark" }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    viewModel.clearForNewSession()
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                }) { Text(text = backText, color = MalaccaTeal) }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
                Column {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = landmarkName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                            Text(text = badgeLabel, color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
                Text(text = responseText, color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(16.dp))
            }

            Button(onClick = { ttsManager.speak(responseText, viewModel.selectedLanguage) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) {
                Text(text = replayText, color = MalaccaTeal, fontWeight = FontWeight.Medium)
            }
            Button(onClick = { viewModel.clearResultForFollowUp(); navController.navigate(ROUTE_LISTENING) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MalaccaTeal)) {
                Text(text = followText, color = TextPrimary, fontWeight = FontWeight.Medium)
            }
            Button(onClick = { viewModel.clearForNewSession(); navController.popBackStack(ROUTE_HOME, inclusive = false) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) {
                Text(text = anotherText, color = TextPrimary, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Restaurant variant ───────────────────────────────────────────────────────

@Composable
private fun RestaurantResultScreen(
    navController: NavController,
    viewModel: GuideViewModel,
    ttsManager: TtsManager
) {
    val restaurant = viewModel.restaurantResult
    val nearby = viewModel.nearbyResult
    val bitmap = viewModel.capturedBitmap

    val responseText = nearby?.response ?: restaurant?.response ?: restaurant?.message ?: "No response received."

    LaunchedEffect(responseText) {
        ttsManager.speak(responseText, viewModel.selectedLanguage)
    }

    // Speak nearby response automatically when it arrives
    LaunchedEffect(nearby) {
        nearby?.response?.let { ttsManager.speak(it, viewModel.selectedLanguage) }
    }

    val backText   = when (viewModel.selectedLanguage) { "ZH" -> "← 返回"; "MS" -> "← Kembali"; else -> "← Back" }
    val replayText = when (viewModel.selectedLanguage) { "ZH" -> "重播"; "MS" -> "Main semula"; else -> "Replay" }
    val nearbyText = when (viewModel.selectedLanguage) { "ZH" -> "附近餐厅"; "MS" -> "Restoran Berdekatan"; else -> "Find Nearby" }
    val anotherText = when (viewModel.selectedLanguage) { "ZH" -> "扫描其他餐厅"; "MS" -> "Imbas restoran lain"; else -> "Scan another restaurant" }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    viewModel.clearForNewSession()
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                }) { Text(text = backText, color = MalaccaTeal) }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Restaurant card (hidden when nearby results are showing)
            if (nearby == null && restaurant != null) {
                Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
                    Column {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = restaurant.restaurantName ?: "Unknown Restaurant", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "★ ${restaurant.rating ?: "-"}", color = MalaccaGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = reviewsLabel(restaurant.reviewCount, viewModel.selectedLanguage), color = TextSecondary, fontSize = 14.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = priceLevelLabel(restaurant.priceLevel, viewModel.selectedLanguage), color = TextSecondary, fontSize = 13.sp)
                                Text(text = "·", color = TextSecondary, fontSize = 13.sp)
                                Text(text = restaurant.cuisine ?: "", color = TextSecondary, fontSize = 13.sp)
                            }
                            Text(text = restaurant.openingHours ?: "", color = if (isOpenNow(restaurant.openingHours, viewModel.selectedLanguage)) SuccessGreen else ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Nearby results list
            if (nearby != null) {
                val nearbyTitle = when (viewModel.selectedLanguage) {
                    "MS" -> "Pilihan Berdekatan"; "ZH" -> "附近的选择"; else -> "Nearby Alternatives"
                }
                Text(text = nearbyTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                nearby.alternatives.forEach { place ->
                    NearbyPlaceCard(place, viewModel.selectedLanguage)
                }
            }

            // Response text box
            Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
                Text(text = responseText, color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(16.dp))
            }

            // Replay button
            Button(
                onClick = { ttsManager.speak(responseText, viewModel.selectedLanguage) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text(text = replayText, color = MalaccaTeal, fontWeight = FontWeight.Medium)
            }

            // Find Nearby button (only shown before nearby results load)
            if (nearby == null && restaurant?.placeId != null) {
                if (viewModel.isSearchingNearby) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MalaccaTeal, strokeWidth = 3.dp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.findNearby(restaurant.placeId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MalaccaTeal)
                    ) {
                        Text(text = nearbyText, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Scan another restaurant
            Button(
                onClick = { viewModel.clearForNewSession(); navController.popBackStack(ROUTE_HOME, inclusive = false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text(text = anotherText, color = TextPrimary, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NearbyPlaceCard(place: PlaceAlternative, language: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = place.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "★ ${place.rating}", color = MalaccaGold, fontSize = 13.sp)
                Text(text = awayLabel(place.distanceM, language), color = TextSecondary, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = priceLevelLabel(place.priceLevel, language), color = TextSecondary, fontSize = 12.sp)
                Text(text = "·", color = TextSecondary, fontSize = 12.sp)
                Text(text = place.cuisine, color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = place.openingHours,
                color = if (isOpenNow(place.openingHours, language)) SuccessGreen else ErrorRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun priceLevelLabel(level: Int?, language: String = "EN") = when (language) {
    "MS" -> when (level) {
        1 -> "Berpatutan"; 2 -> "Harga sederhana"; 3 -> "Mahal"; 4 -> "Sangat mahal"
        else -> "Harga tidak dinyatakan"
    }
    "ZH" -> when (level) {
        1 -> "经济实惠"; 2 -> "中等价格"; 3 -> "偏贵"; 4 -> "非常昂贵"
        else -> "未列出价格"
    }
    else -> when (level) {
        1 -> "Affordable"; 2 -> "Moderately priced"; 3 -> "Pricey"; 4 -> "Very expensive"
        else -> "Price not listed"
    }
}

private fun reviewsLabel(count: Int?, language: String) = when (language) {
    "MS" -> "${count ?: 0} ulasan"
    "ZH" -> "${count ?: 0} 条评论"
    else -> "${count ?: 0} reviews"
}

private fun isOpenNow(hours: String?, language: String): Boolean {
    val h = hours ?: return false
    return when (language) {
        "MS" -> h.contains("dibuka", ignoreCase = true)
        "ZH" -> h.contains("营业")
        else -> h.contains("Open", ignoreCase = true)
    }
}

private fun awayLabel(distanceM: Int, language: String) = when (language) {
    "MS" -> "${distanceM}m dari sini"
    "ZH" -> "${distanceM}米"
    else -> "${distanceM}m away"
}
