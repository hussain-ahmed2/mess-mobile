package com.messmate.bd.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime language switch between **English** and **বাংলা**.
 *
 * * On API 33+ the platform [android.app.LocaleManager] is used, so the
 *   language also shows up in the Android system settings ("App languages").
 * * On older versions the base context of the activity is wrapped with a
 *   configuration that carries the selected locale – the classic approach that
 *   works down to API 21.
 */
@Singleton
class LocaleController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The selected language tag (`en`, `bn`) – empty means "follow the system". */
    fun currentTag(): String = prefs.getString(KEY_TAG, "") ?: ""

    /** `true` when the app runs in Bangla. */
    fun isBangla(): Boolean = currentTag().startsWith("bn", ignoreCase = true)

    /**
     * Stores the language and pushes it to the platform.
     *
     * @param tag `en`, `bn` or an empty string to follow the system.
     */
    fun setLanguage(tag: String) {
        prefs.edit().putString(KEY_TAG, tag).apply()
        applyToPlatform()
    }

    /** Keeps the platform locale list in sync with the stored preference. */
    fun applyToPlatform() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
        manager.applicationLocales = LocaleList.forLanguageTags(currentTag())
    }

    companion object {
        private const val PREFS_NAME = "messmate_locale"
        private const val KEY_TAG = "language_tag"

        const val TAG_ENGLISH = "en"
        const val TAG_BANGLA = "bn"

        /**
         * Wraps a context so that all resources are resolved in the selected
         * language. Called from `Activity.attachBaseContext`.
         */
        fun wrap(context: Context): Context {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val tag = prefs.getString(KEY_TAG, "") ?: ""
            if (tag.isEmpty()) return context

            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)

            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        }
    }
}
