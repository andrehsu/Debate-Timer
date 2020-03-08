package andre.debatetimer

import andre.debatetimer.livedata.SharedPreferenceLiveData
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

object Prefs {
    private val LogTag = Prefs::class.java.simpleName
    
    private var initialized = false
    
    fun init(context: Context) {
        if (!initialized) {
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
            
            initialized = true
        } else {
            Log.d(LogTag, "Already initialized")
        }
    }
    
    lateinit var enableBells: SharedPreferenceLiveData<Boolean>
    
    lateinit var countMode: SharedPreferenceLiveData<CountMode>
}