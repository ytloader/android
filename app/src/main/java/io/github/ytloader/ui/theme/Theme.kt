package io.github.ytloader.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Windows 데스크톱판(WPF-UI, Fluent)과 같은 색 토큰. 강조색은 앱 아이콘의 빨강 #EB4B3C에서 파생됐다.
 * 색을 바꿀 때는 [DarkTokens]·[LightTokens]만 고친다. 기기 배경화면 색(Material You)은 쓰지 않는다.
 */
private class Tokens(
    val accentFill: Color, // 주 버튼, 스위치 켜짐, 선택 상태, 진행 바
    val onAccent: Color, // 강조 채움 위 글자·아이콘
    val accentText: Color, // 링크 등 강조 텍스트
    val background: Color,
    val bottomBar: Color,
    val card: Color,
    val cardStroke: Color,
    val control: Color, // 입력란, 드롭다운
    val subtleFill: Color, // 칩, 로딩 스켈레톤, 눌림
    val divider: Color,
    val strongStroke: Color, // 꺼진 스위치 테두리 등
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val success: Color,
    val successBg: Color,
    val error: Color,
    val errorBg: Color,
    val warning: Color,
    val warningBg: Color,
)

private val DarkTokens = Tokens(
    accentFill = Color(0xFFFFBAB4),
    onAccent = Color(0xFF000000),
    accentText = Color(0xFFFFBAB4),
    background = Color(0xFF202020),
    bottomBar = Color(0xFF282828),
    card = Color(0xFF2B2B2B),
    cardStroke = Color(0xFF1D1D1D),
    control = Color(0xFF373737),
    subtleFill = Color(0xFF373737),
    divider = Color(0xFF3C3C3C),
    strongStroke = Color(0xFF9F9F9F),
    text1 = Color(0xFFFFFFFF),
    text2 = Color(0xFFCFCFCF),
    text3 = Color(0xFF9B9B9B),
    success = Color(0xFF6CCB5F),
    successBg = Color(0xFF393D1B),
    error = Color(0xFFFF99A4),
    errorBg = Color(0xFF442726),
    warning = Color(0xFFFCE100),
    warningBg = Color(0xFF433519),
)

private val LightTokens = Tokens(
    accentFill = Color(0xFFD24335),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFAB372C),
    background = Color(0xFFF3F3F3),
    bottomBar = Color(0xFFF9F9F9),
    card = Color(0xFFFBFBFB),
    cardStroke = Color(0xFFE5E5E5),
    control = Color(0xFFFEFEFE),
    subtleFill = Color(0xFFF2F2F2),
    divider = Color(0xFFECECEC),
    strongStroke = Color(0xFF8B8B8B),
    text1 = Color(0xFF1B1B1B),
    text2 = Color(0xFF5F5F5F),
    text3 = Color(0xFF8B8B8B),
    success = Color(0xFF0F7B0F),
    successBg = Color(0xFFDFF6DD),
    error = Color(0xFFC42B1C),
    errorBg = Color(0xFFFDE7E9),
    warning = Color(0xFF9D5D00),
    warningBg = Color(0xFFFFF4CE),
)

// 모든 역할을 직접 지정하므로 lightColorScheme의 기본값은 다크에서도 쓰이지 않는다.
// 표면 단계: Lowest·Low = 배경(시트 배경), Container = 하단 바, High = 카드(카드·대화상자), Highest = 입력 컨트롤(꺼진 스위치 트랙).
// secondaryContainer(진행 바 트랙, 톤 버튼)와 surfaceVariant(썸네일 자리)는 보조 채움.
private fun Tokens.colorScheme(inverse: Tokens): ColorScheme = lightColorScheme(
    primary = accentFill,
    onPrimary = onAccent,
    primaryContainer = card,
    onPrimaryContainer = text1,
    inversePrimary = inverse.accentFill,
    secondary = accentFill,
    onSecondary = onAccent,
    secondaryContainer = subtleFill,
    onSecondaryContainer = text1,
    tertiary = accentFill,
    onTertiary = onAccent,
    tertiaryContainer = subtleFill,
    onTertiaryContainer = text1,
    background = background,
    onBackground = text1,
    surface = card,
    onSurface = text1,
    surfaceVariant = subtleFill,
    onSurfaceVariant = text2,
    surfaceTint = card, // = surface. Fluent처럼 고도(tonalElevation)에 색을 섞지 않는다
    inverseSurface = inverse.background,
    inverseOnSurface = inverse.text1,
    error = error,
    onError = onAccent,
    errorContainer = errorBg,
    onErrorContainer = text1,
    outline = strongStroke,
    outlineVariant = divider,
    scrim = Color.Black,
    surfaceBright = control,
    surfaceDim = background,
    surfaceContainerLowest = background,
    surfaceContainerLow = background,
    surfaceContainer = bottomBar,
    surfaceContainerHigh = card,
    surfaceContainerHighest = control,
)

/** ColorScheme에 자리가 없는 토큰. [YTLoaderTheme.colors]로 읽는다. */
@Immutable
class ExtendedColors(
    val accentText: Color,
    val cardStroke: Color,
    val textTertiary: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
)

private fun Tokens.extended() = ExtendedColors(
    accentText = accentText,
    cardStroke = cardStroke,
    textTertiary = text3,
    success = success,
    successContainer = successBg,
    warning = warning,
    warningContainer = warningBg,
)

private val DarkColors = DarkTokens.colorScheme(inverse = LightTokens)
private val LightColors = LightTokens.colorScheme(inverse = DarkTokens)
private val DarkExtended = DarkTokens.extended()
private val LightExtended = LightTokens.extended()

private val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

// 카드 8dp, 칩·입력 컨트롤 4dp (Fluent). 대화상자·시트(extraLarge)도 Fluent 오버레이처럼 8dp.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

// 큰 제목 16sp SemiBold, 본문 14sp, 라벨·캡션 12sp(bodySmall·labelMedium 기본값 그대로).
private val AppTypography = Typography().run {
    copy(
        titleLarge = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyMedium,
    )
}

object YTLoaderTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current

    /** 카드 1dp 테두리. */
    val cardBorder: BorderStroke
        @Composable get() = BorderStroke(1.dp, colors.cardStroke)
}

/** 글자·외곽선 버튼용. 라이트의 강조 채움(primary)은 글자로 쓰면 대비가 4.5:1에 못 미쳐 강조 글자색을 쓴다. */
@Composable
fun accentTextButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(contentColor = YTLoaderTheme.colors.accentText)

@Composable
fun accentOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = YTLoaderTheme.colors.accentText)

@Composable
fun YTLoaderTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
