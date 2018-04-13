package andre.debatetimer

import andre.debatetimer.extensions.defaultSharedPreferences
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap

object Prefs : SharedPreferences.OnSharedPreferenceChangeListener {
	private val LogTag = Prefs::class.java.simpleName
	
	private var initialized = false
	private inline fun <R> requireInitialized(block: () -> R): R {
		require(initialized) { "Prefs not initialized" }
		return block()
	}
	
	lateinit var pref_bell_enabled_key: String
		private set
	var pref_bell_enabled_default = false
		get() = requireInitialized { return field }
		private set
	
	lateinit var pref_count_mode: String
		private set
	lateinit var pref_count_mode_default: String
		private set
	
	private val cache = ConcurrentHashMap<String, Any>()
	private lateinit var sharedPreferences: SharedPreferences
	
	fun init(context: Context) {
		if (!initialized) {
			with(context.resources) {
				pref_bell_enabled_key = getString(R.string.pref_bell_enabled_key)
				pref_bell_enabled_default = getBoolean(R.bool.pref_bell_enabled_default)
				pref_count_mode = getString(R.string.pref_count_mode)
				pref_count_mode_default = getString(R.string.pref_count_mode_default)
			}
			sharedPreferences = context.defaultSharedPreferences
			
			initialized = true
		} else {
			Log.d(LogTag, "Already initialized")
		}
	}
	
	var debateBellEnabled: Boolean
		get() = requireInitialized {
			return cache.getOrPut(pref_bell_enabled_key)
			{ sharedPreferences.getBoolean(pref_bell_enabled_key, pref_bell_enabled_default) } as Boolean
		}
		set(value) {
			requireInitialized {
				cache[pref_bell_enabled_key] = value
				sharedPreferences.edit()
						.putBoolean(pref_bell_enabled_key, value)
						.apply()
			}
		}
	
	var countMode: CountMode
		get() = requireInitialized {
			return cache.getOrPut(pref_count_mode) {
				CountMode.parseString(sharedPreferences.getString(pref_count_mode, pref_count_mode_default))
			} as CountMode
		}
		set(value) {
			requireInitialized {
				cache[pref_count_mode] = value
				sharedPreferences.edit {
					putString(pref_count_mode, value.toString())
				}
			}
		}
	
	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		//invalidates instead of updating
		when (key) {
			pref_bell_enabled_key -> cache.remove(key)
		}
	}
}