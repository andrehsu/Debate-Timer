package andre.debatetimer.extensions

import android.content.Context
import android.preference.PreferenceManager

val Context.defaultSharedPreferences
	get() = PreferenceManager.getDefaultSharedPreferences(this)