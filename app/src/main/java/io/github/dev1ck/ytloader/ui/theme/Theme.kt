package io.github.dev1ck.ytloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 고정 브랜드 색 (아이콘의 코랄~레드 계열). 기기 배경화면 색(Material You)은 쓰지 않는다.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB7282E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410004),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EF),
    surfaceContainer = Color(0xFFFCEAE8),
    surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC),
)

// 다크: 표면은 붉은 기 없는 중립 회색, 브랜드 레드는 강조(선택 상태·스위치·버튼)에만 쓴다.
// 표면 단계(배경 → 카드 → 하단 바 → 시트 카드)를 일반 M3보다 넓게 벌려 층이 보이게 한다.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AD),
    onPrimary = Color(0xFF680010),
    primaryContainer = Color(0xFF930F1B),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB3AD),
    onSecondary = Color(0xFF680010),
    secondaryContainer = Color(0xFF7A1A22),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFDFC38C),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF574419),
    onTertiaryContainer = Color(0xFFFCDFA6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE8E6E6),
    surfaceVariant = Color(0xFF3A3838),
    onSurfaceVariant = Color(0xFFC4C0C0),
    outline = Color(0xFF8E8A8A),
    outlineVariant = Color(0xFF424040),
    surfaceContainerLowest = Color(0xFF0C0C0C),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF363636),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun YTLoaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
