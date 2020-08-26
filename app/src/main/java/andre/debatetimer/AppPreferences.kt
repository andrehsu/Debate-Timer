package andre.debatetimer

import andre.debatetimer.livedata.SharedPreferenceLiveData
import andre.debatetimer.timer.TimerConfiguration
import android.content.Context
import androidx.preference.PreferenceManager

class AppPreferences private constructor(context: Context) {
    val enableBells: SharedPreferenceLiveData<Boolean>
    val countMode: SharedPreferenceLiveData<CountMode>
    val timersStr: SharedPreferenceLiveData<String>
    val selectedTimerConfig: SharedPreferenceLiveData<TimerConfiguration>
    
    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        enableBells = SharedPreferenceLiveData.ofBoolean(
                sp,
                context.getString(R.string.pref_enable_bells_key),
                context.resources.getBoolean(R.bool.pref_enable_bells_default).toString()
        )
        countMode = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_count_mode_key),
                context.getString(R.string.pref_count_mode_default),
                { CountMode.fromString(it) },
                { it.toString() }
        )
        timersStr = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_timers_key), context.getString(R.string.pref_timers_default), { it }, { it }
        )
        selectedTimerConfig = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_selected_timer_config_key),
                context.getString(R.string.pref_selected_timer_config_default),
                { TimerConfiguration.parseTag(it) },
                { it.tag }
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