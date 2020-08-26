package andre.debatetimer

import andre.debatetimer.livedata.SharedPreferenceLiveData
import android.content.Context
import androidx.preference.PreferenceManager

class AppPreference private constructor(context: Context) {
    val enableBells: SharedPreferenceLiveData<Boolean>
    val countMode: SharedPreferenceLiveData<CountMode>
    val timersStr: SharedPreferenceLiveData<String>
    
    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        enableBells = SharedPreferenceLiveData.ofBoolean(
                sp,
                context.getString(R.string.pref_enable_bells_key),
                context.resources.getBoolean(R.bool.pref_enable_bells_default).toString()
        )
        countMode = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_count_mode),
                context.getString(R.string.pref_count_mode_default),
                { CountMode.fromString(it) },
                { it.toString() }
        )
        timersStr = SharedPreferenceLiveData.ofString(
                sp,
                context.getString(R.string.pref_timers_key), context.getString(R.string.pref_timers_default), { it }, { it }
        )
        
        
    }
    
    companion object {
        private var instance: AppPreference? = null
        
        @Synchronized
        fun getInstance(context: Context): AppPreference {
            if (instance == null) {
                instance = AppPreference(context)
            }
            
            return instance!!
        }
    }
}