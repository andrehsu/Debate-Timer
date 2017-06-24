package andre.debatetimer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object Prefs {
	private val LogTag = Prefs::class.java.simpleName
	
	
	private inline fun <R> requireInitialized(block: () -> R): R {
		require(initialized) { "Prefs not initialized" }
		return block()
	}
	
	private var initialized = false
	
	lateinit var pref_bell_enabled_key: String
		private set
	var pref_bell_enabled_default = false
		get() = requireInitialized { return field }
		private set
	
	fun init(context: Context) {
		if (!initialized) {
			with(context.resources) {
				pref_bell_enabled_key = getString(R.string.pref_bell_enabled_key)
				pref_bell_enabled_default = getBoolean(R.bool.pref_bell_enabled_default)
			}
			initialized = true
		} else {
			Log.d(LogTag, "Already initialized")
		}
	}
	
	fun getDebateBellEnabled(sharedPreferences: SharedPreferences): Boolean = requireInitialized {
		sharedPreferences.getBoolean(pref_bell_enabled_key, pref_bell_enabled_default)
	}
	
	
	fun setDebateBellEnabled(enabled: Boolean, sharedPreferences: SharedPreferences) = requireInitialized {
		sharedPreferences.edit()
				.putBoolean(pref_bell_enabled_key, enabled)
				.apply()
	}
}