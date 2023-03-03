package eu.kanade.tachiyomi.lib.useragentmodifier

import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.ConfigurableSource

interface UserAgentModifier : ConfigurableSource {
    /**
     * Override this property with the shared preferences of the source.
     */
    val preferences: SharedPreferences

    /**
     * The client should be used to obtain this property.
     * Use: client.interceptors.toString().contains("UserAgentInterceptor")
     */
    val hasUaIntercept: Boolean

    /**
     * Override this property with the value null.
     */
    var userAgent: String?

    /**
     * Override this property with the value false.
     */
    var checkedUa: Boolean

    /**
     * Override this property with the value true to use a random user agent by default.
     * This is a optional property that defaults false.
     */
    val useRandomUserAgentByDefault: Boolean
        get() = false

    /**
     * Override with a list of Strings that must be present in the user agent.
     * Example: listOf("chrome"), listOf("linux", "windows") and listOf("108")
     * This is a optional property that defaults to an empty list.
     */
    val filterIncludeUserAgent: List<String>
        get() = listOf()

    /**
     * Override with a list of Strings that should not be present in the user-agent.
     * Example: listOf("chrome"), listOf("linux", "windows") and listOf("108")
     * This is an optional property and defaults to an empty list.
     */
    val filterExcludeUserAgent: List<String>
        get() = listOf()

    /**
     * This methods overrides ConfigurableSource interface to add preferences to set a custom and random user agent by default.
     * AddRandomAndCustomUserAgentPreferences() should be called from inside the method when this one is overridden.
     */
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        addRandomAndCustomUserAgentPreferences(screen)
    }
}
