package com.hussain.messmobile

import androidx.compose.ui.graphics.Color
import com.hussain.messmobile.ui.navigation.TabDestination
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.DarkAppColors
import com.hussain.messmobile.ui.theme.LightAppColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun lightPaletteMatchesTheDesignTokens() {
        assertEquals(Color(0xFF000000), LightAppColors.text)
        assertEquals(Color(0xFFFFFFFF), LightAppColors.background)
        assertEquals(Color(0xFFF0F0F3), LightAppColors.backgroundElement)
        assertEquals(Color(0xFFE0E1E6), LightAppColors.backgroundSelected)
        assertEquals(Color(0xFF60646C), LightAppColors.textSecondary)
    }

    @Test
    fun darkPaletteMatchesTheDesignTokens() {
        assertEquals(Color(0xFFFFFFFF), DarkAppColors.text)
        assertEquals(Color(0xFF000000), DarkAppColors.background)
        assertEquals(Color(0xFF212225), DarkAppColors.backgroundElement)
        assertEquals(Color(0xFF2E3135), DarkAppColors.backgroundSelected)
        assertEquals(Color(0xFFB0B4BA), DarkAppColors.textSecondary)
    }

    @Test
    fun lightAndDarkPalettesDiffer() {
        assertNotEquals(LightAppColors.text, DarkAppColors.text)
        assertNotEquals(LightAppColors.background, DarkAppColors.background)
    }

    @Test
    fun textStylesKeepTheirSizes() {
        assertEquals(48f, AppTextStyle.Title.fontSize.value, 0.01f)
        assertEquals(32f, AppTextStyle.Subtitle.fontSize.value, 0.01f)
        assertEquals(16f, AppTextStyle.Default.fontSize.value, 0.01f)
        assertEquals(14f, AppTextStyle.Small.fontSize.value, 0.01f)
        assertEquals(12f, AppTextStyle.Code.fontSize.value, 0.01f)
    }
}

class TabDestinationTest {

    @Test
    fun everyTabHasItsOwnRoute() {
        val routes = TabDestination.entries.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun everyTabHasALabel() {
        TabDestination.entries.forEach { destination ->
            assertTrue(destination.label.isNotBlank())
        }
    }
}
