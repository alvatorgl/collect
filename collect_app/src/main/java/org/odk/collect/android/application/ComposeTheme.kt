package org.odk.collect.android.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.odk.collect.android.R
import org.odk.collect.androidshared.ui.ComposeThemeProvider

private val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val UnboundedFamily = FontFamily(
    Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold)
)

private fun TextStyle.asInter(weight: FontWeight = fontWeight ?: FontWeight.Normal): TextStyle {
    return copy(fontFamily = InterFamily, fontWeight = weight)
}

private fun TextStyle.asUnbounded(): TextStyle {
    return copy(fontFamily = UnboundedFamily, fontWeight = FontWeight.Bold)
}

/**
 * Changes to this theme should also be made in `theme.xml`
 */
@Composable
fun CollectTheme(content: @Composable () -> Unit) {
    val lightColors = lightColorScheme(
        primary = colorResource(R.color.colorPrimaryLight),
        onPrimary = colorResource(R.color.colorOnPrimaryLight),
        primaryContainer = colorResource(R.color.colorPrimaryContainerLight),
        onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainerLight),
        secondary = colorResource(R.color.colorSecondaryLight),
        onSecondary = colorResource(R.color.colorOnSecondaryLight),
        secondaryContainer = colorResource(R.color.colorSecondaryContainerLight),
        onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainerLight),
        tertiary = colorResource(R.color.colorTertiaryLight),
        onTertiary = colorResource(R.color.colorOnTertiaryLight),
        tertiaryContainer = colorResource(R.color.colorTertiaryContainerLight),
        onTertiaryContainer = colorResource(R.color.colorOnTertiaryContainerLight),
        background = colorResource(R.color.colorBackgroundLight),
        onBackground = colorResource(R.color.colorOnSurfaceLight),
        surface = colorResource(R.color.colorSurfaceLight),
        onSurface = colorResource(R.color.colorOnSurfaceLight),
        surfaceVariant = colorResource(R.color.colorSurfaceVariantLight),
        onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariantLight),
        outline = colorResource(R.color.colorOutlineLight),
        error = colorResource(R.color.colorErrorLight),
        onError = colorResource(R.color.colorOnErrorLight),
        surfaceContainerLowest = colorResource(R.color.colorSurfaceContainerLowestLight),
        surfaceContainerLow = colorResource(R.color.colorSurfaceContainerLowLight),
        surfaceContainer = colorResource(R.color.colorSurfaceContainerLight),
        surfaceContainerHigh = colorResource(R.color.colorSurfaceContainerHighLight),
        surfaceContainerHighest = colorResource(R.color.colorSurfaceContainerHighestLight)
    )

    val darkColors = darkColorScheme(
        primary = colorResource(R.color.colorPrimaryDark),
        onPrimary = colorResource(R.color.colorOnPrimaryDark),
        primaryContainer = colorResource(R.color.colorPrimaryContainerDark),
        onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainerDark),
        secondary = colorResource(R.color.colorSecondaryDark),
        onSecondary = colorResource(R.color.colorOnSecondaryDark),
        secondaryContainer = colorResource(R.color.colorSecondaryContainerDark),
        onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainerDark),
        tertiary = colorResource(R.color.colorTertiaryDark),
        onTertiary = colorResource(R.color.colorOnTertiaryDark),
        tertiaryContainer = colorResource(R.color.colorTertiaryContainerDark),
        onTertiaryContainer = colorResource(R.color.colorOnTertiaryContainerDark),
        background = colorResource(R.color.colorBackgroundDark),
        onBackground = colorResource(R.color.colorOnSurfaceDark),
        surface = colorResource(R.color.colorSurfaceDark),
        onSurface = colorResource(R.color.colorOnSurfaceDark),
        surfaceVariant = colorResource(R.color.colorSurfaceVariantDark),
        onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariantDark),
        outline = colorResource(R.color.colorOutlineDark),
        error = colorResource(R.color.colorErrorDark),
        onError = colorResource(R.color.colorOnErrorDark),
        surfaceContainerLowest = colorResource(R.color.colorSurfaceContainerLowestDark),
        surfaceContainerLow = colorResource(R.color.colorSurfaceContainerLowDark),
        surfaceContainer = colorResource(R.color.colorSurfaceContainerDark),
        surfaceContainerHigh = colorResource(R.color.colorSurfaceContainerHighDark),
        surfaceContainerHighest = colorResource(R.color.colorSurfaceContainerHighestDark)
    )

    val colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors

    val defaults = MaterialTheme.typography
    val typography = Typography(
        displayLarge = defaults.displayLarge.asUnbounded(),
        displayMedium = defaults.displayMedium.asUnbounded(),
        displaySmall = defaults.displaySmall.asUnbounded(),
        headlineLarge = defaults.headlineLarge.asUnbounded(),
        headlineMedium = defaults.headlineMedium.asUnbounded(),
        headlineSmall = defaults.headlineSmall.asUnbounded(),
        titleLarge = defaults.titleLarge.asInter(FontWeight.Medium).copy(
            fontSize = 20.sp,
            lineHeight = 24.sp
        ),
        titleMedium = defaults.titleMedium.asInter(FontWeight.Medium),
        titleSmall = defaults.titleSmall.asInter(FontWeight.Medium),
        bodyLarge = defaults.bodyLarge.asInter(),
        bodyMedium = defaults.bodyMedium.asInter(),
        bodySmall = defaults.bodySmall.asInter(),
        labelLarge = defaults.labelLarge.asInter(FontWeight.Medium),
        labelMedium = defaults.labelMedium.asInter(FontWeight.Medium),
        labelSmall = defaults.labelSmall.asInter(FontWeight.Medium)
    )

    val shapes = Shapes(
        small = MaterialTheme.shapes.small,
        medium = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerSize)),
        large = MaterialTheme.shapes.large
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

interface CollectComposeThemeProvider : ComposeThemeProvider {
    @Composable
    override fun Theme(content: @Composable () -> Unit) {
        CollectTheme { content() }
    }
}
