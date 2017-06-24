package andre.debatetimer

import android.content.Context
import android.content.SharedPreferences

object Prefs {
	lateinit var pref_bell_enabled_key: String
		private set
	var pref_bell_enabled_default = false
		private set
	
	fun init(context: Context) {
		with(context.resources) {
			pref_bell_enabled_key = getString(R.string.pref_bell_enabled_key)
			pref_bell_enabled_default = getBoolean(R.bool.pref_bell_enabled_default)
		}
	}
	
	fun getDebateBellEnabled(sharedPreferences: SharedPreferences) =
			sharedPreferences.getBoolean(pref_bell_enabled_key, pref_bell_enabled_default)
	
	fun setDebateBellEnabled(enabled: Boolean, sharedPreferences: SharedPreferences) {
		sharedPreferences.edit()
				.putBoolean(pref_bell_enabled_key, enabled)
				.apply()
	}
}