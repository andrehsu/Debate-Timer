package org.debatetimer

import android.content.Context
import androidx.preference.PreferenceManager
import org.debatetimer.livedata.SharedPreferenceLiveData

class AppPreferences private constructor(context: Context) {
    val enableBells: SharedPreferenceLiveData<Boolean>
    val countMode: SharedPreferenceLiveData<CountMode>
    val timersStr: SharedPreferenceLiveData<String>
    val selectedTimerConfigTag: SharedPreferenceLiveData<String>
    
    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        enableBells = SharedPreferenceLiveData.ofBoolean(
                sp,
                context.getString(R.string.pref_enable_bells_key),
                context.resources.getBoolean(R.bool.pref_enable_bells_default).toString()
        )
        countMode = SharedPreferenceLiveData.ofObject(
                sp,
                context.getString(R.string.pref_count_mode_key),
                context.getString(R.string.pref_count_mode_default),
                { CountMode.fromString(it) },
                { it.toString() }
        )
        timersStr = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_timers_key), context.getString(R.string.pref_timers_default)
        )
        selectedTimerConfigTag = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_selected_timer_config_key),
                context.getString(R.string.pref_selected_timer_config_default)
        )
    }
    
    companion object {
        private var instance: AppPreferences? = null
        
        @Synchronized
        fun getInstance(context: Context): AppPreferences {
            if (instance == null) {
                instance = AppPreferences(context)
            }
            
            return instance!!
        }
    }
}