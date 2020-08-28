package org.debatetimer

import android.content.Context
import androidx.preference.PreferenceManager
import org.debatetimer.livedata.SharedPreferenceLiveData
import org.debatetimer.livedata.getBooleanLiveData
import org.debatetimer.livedata.getObjectLiveData
import org.debatetimer.livedata.getStringLiveData
import org.debatetimer.timer.TimerConfiguration


class AppPreferences private constructor(context: Context) {
    val enableBells: SharedPreferenceLiveData<Boolean>
    val countMode: SharedPreferenceLiveData<CountMode>
    val timerConfigs: SharedPreferenceLiveData<Map<String, TimerConfiguration>>
    val selectedTimerConfigTag: SharedPreferenceLiveData<String>
    
    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        with(context.resources) {
            enableBells = sp.getBooleanLiveData(
                    getString(R.string.pref_enable_bells_key),
                    getBoolean(R.bool.pref_enable_bells_default)
            )
            countMode = sp.getObjectLiveData(
                    getString(R.string.pref_count_mode_key),
                    getString(R.string.pref_count_mode_default),
                    { CountMode.fromString(it) },
                    { it.toString() }
            )
            timerConfigs = sp.getObjectLiveData(
                    getString(R.string.pref_timers_key),
                    getString(R.string.pref_timers_default),
                    {
                        it.split('|').map { s ->
                            val timerOption = TimerConfiguration.parseTag(s)
                
                            timerOption.tag to timerOption
                        }.toMap()
                    },
                    { it.map { it.value.toString() }.joinToString("|") }
            )
            selectedTimerConfigTag = sp.getStringLiveData(
                    getString(R.string.pref_selected_timer_config_key),
                    getString(R.string.pref_selected_timer_config_default),
            )
        }
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