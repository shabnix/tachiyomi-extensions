package eu.kanade.tachiyomi.lib.useragentmodifier

import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.PREF_KEY_CUSTOM_UA
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.PREF_KEY_RANDOM_UA
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.RESTART_APP_STRING
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.SUMMARY_CLEANING_CUSTOM_UA
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.TITLE_CUSTOM_UA
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.TITLE_RANDOM_UA
import eu.kanade.tachiyomi.lib.useragentmodifier.UserAgentModifierConstants.UA_DB_URL
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

fun UserAgentModifier.uaInterceptor(chain: Interceptor.Chain): Response {
    val useRandomUa = preferences.getBoolean(PREF_KEY_RANDOM_UA, false)
    val customUa = preferences.getString(PREF_KEY_CUSTOM_UA, "")

    try {
        if (hasUaIntercept && (useRandomUa || customUa!!.isNotBlank())) {
            Log.i(
                "Extension_setting",
                "$TITLE_RANDOM_UA or $TITLE_CUSTOM_UA option is ENABLED",
            )

            if (customUa!!.isNotBlank() && useRandomUa.not()) {
                userAgent = customUa
            }

            if (userAgent.isNullOrBlank() && !checkedUa) {
                val uaResponse = chain.proceed(GET(UA_DB_URL))

                if (uaResponse.isSuccessful) {
                    var listUserAgentString =
                        Json.decodeFromString<Map<String, List<String>>>(uaResponse.body.string())["desktop"]

                    if (filterIncludeUserAgent.isNotEmpty()) {
                        listUserAgentString = listUserAgentString!!.filter {
                            filterIncludeUserAgent.any { filter ->
                                it.contains(filter, ignoreCase = true)
                            }
                        }
                    }
                    if (filterExcludeUserAgent.isNotEmpty()) {
                        listUserAgentString = listUserAgentString!!.filterNot {
                            filterExcludeUserAgent.any { filter ->
                                it.contains(filter, ignoreCase = true)
                            }
                        }
                    }
                    userAgent = listUserAgentString!!.random()
                    checkedUa = true
                }

                uaResponse.close()
            }

            if (userAgent.isNullOrBlank().not()) {
                val newRequest = chain.request().newBuilder()
                    .header("User-Agent", userAgent!!.trim())
                    .build()

                return chain.proceed(newRequest)
            }
        }

        return chain.proceed(chain.request())
    } catch (e: Exception) {
        throw IOException(e.message)
    }
}

fun UserAgentModifier.addRandomAndCustomUserAgentPreferences(screen: PreferenceScreen) {
    if (!hasUaIntercept) {
        return // Unable to change the user agent. Therefore the preferences won't be displayed.
    }

    val prefRandomUserAgent = SwitchPreferenceCompat(screen.context).apply {
        key = PREF_KEY_RANDOM_UA
        title = TITLE_RANDOM_UA
        summary = if (preferences.getBoolean(PREF_KEY_RANDOM_UA, useRandomUserAgentByDefault)) userAgent else ""
        setDefaultValue(useRandomUserAgentByDefault)
    }

    val prefCustomUserAgent = EditTextPreference(screen.context).apply {
        key = PREF_KEY_CUSTOM_UA
        title = TITLE_CUSTOM_UA
        summary = preferences.getString(PREF_KEY_CUSTOM_UA, "")!!.trim()
    }

    prefRandomUserAgent.setOnPreferenceChangeListener { _, newValue ->
        val useRandomUa = newValue as Boolean
        preferences.edit().putBoolean(PREF_KEY_RANDOM_UA, useRandomUa).apply()
        if (!useRandomUa) {
            Toast.makeText(screen.context, RESTART_APP_STRING, Toast.LENGTH_LONG).show()
        } else {
            userAgent = null
            if (preferences.getString(PREF_KEY_CUSTOM_UA, "").isNullOrBlank().not()) {
                Toast.makeText(screen.context, SUMMARY_CLEANING_CUSTOM_UA, Toast.LENGTH_LONG).show()
            }
        }

        preferences.edit().putString(PREF_KEY_CUSTOM_UA, "").apply()
        prefCustomUserAgent.summary = ""
        true
    }

    prefCustomUserAgent.setOnPreferenceChangeListener { _, newValue ->
        val customUa = newValue as String
        preferences.edit().putString(PREF_KEY_CUSTOM_UA, customUa).apply()
        if (customUa.isBlank()) {
            Toast.makeText(screen.context, RESTART_APP_STRING, Toast.LENGTH_LONG).show()
        } else {
            userAgent = null
        }
        prefCustomUserAgent.summary = customUa.trim()
        prefRandomUserAgent.summary = ""
        prefRandomUserAgent.isChecked = false
        true
    }

    screen.addPreference(prefRandomUserAgent)
    screen.addPreference(prefCustomUserAgent)
}

object UserAgentModifierConstants {
    const val TITLE_RANDOM_UA = "Use Random Latest User-Agent"
    const val PREF_KEY_RANDOM_UA = "pref_key_random_ua"
    const val TITLE_CUSTOM_UA = "Custom User-Agent"
    const val PREF_KEY_CUSTOM_UA = "pref_key_custom_ua"
    const val SUMMARY_CLEANING_CUSTOM_UA = "$TITLE_CUSTOM_UA cleared."
    const val RESTART_APP_STRING = "Restart Tachiyomi to apply new setting."
    const val UA_DB_URL = "https://tachiyomiorg.github.io/user-agents/user-agents.json"
}


