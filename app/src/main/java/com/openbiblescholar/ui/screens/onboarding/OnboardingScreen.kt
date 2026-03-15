package com.openbiblescholar.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.services.sword.SwordModuleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val moduleManager: SwordModuleManager
) : ViewModel() {

    private val _appReady = MutableStateFlow(false)
    val appReady: StateFlow<Boolean> = _appReady.asStateFlow()

    init {
        viewModelScope.launch {
            moduleManager.seedDefaultModules()
            delay(500) // brief splash
            _appReady.value = true
        }
    }
}

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Outlined.MenuBook, "Welcome to OpenBible Scholar",
            "The deepest free Bible study app for Android. Offline-first, no subscriptions, no ads.",
            Color(0xFFC8922A)),
        OnboardingPage(Icons.Outlined.WifiOff, "100% Offline",
            "Read, search, highlight, take notes, and listen — all without an internet connection.",
            Color(0xFF1565C0)),
        OnboardingPage(Icons.Outlined.LibraryBooks, "Huge Free Library",
            "Download KJV, ASV, Strong's Lexicons, Matthew Henry Commentary, Church Fathers, and more — all free, all public domain.",
            Color(0xFF2E7D32)),
        OnboardingPage(Icons.Outlined.VolumeUp, "Text-to-Speech Built In",
            "Have the Bible read to you with word-by-word karaoke highlighting. Adjustable speed, pitch, and voice — all offline.",
            Color(0xFF6A1B9A)),
        OnboardingPage(Icons.Outlined.AutoAwesome, "AI-Powered Study (Optional)",
            "Get contextual explanations, cross-references, passage guides, and sermon outlines — using your free Groq or OpenRouter API key.",
            Color(0xFFD32F2F))
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                OnboardingPageView(page = pages[index])
            }

            // Page indicators
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == i) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.weight(2f)
                ) {
                    Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
                    if (pagerState.currentPage == pages.size - 1) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageView(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(page.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = page.accent,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
