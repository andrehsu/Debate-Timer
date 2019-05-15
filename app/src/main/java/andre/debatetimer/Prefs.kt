package andre.debatetimer

import andre.debatetimer.extensions.defaultSharedPreferences
import andre.debatetimer.livedata.SharedPreferenceLiveData
import android.content.Context
import android.util.Log

object Prefs {
    private val LogTag = Prefs::class.java.simpleName
    
    private var initialized = false
    
    fun init(context: Context) {
        if (!initialized) {
            val sp = context.defaultSharedPreferences
            debateBellEnabled = SharedPreferenceLiveData.ofBoolean(
                    sp,
                    context.getString(R.string.pref_bell_enabled_key),
                    context.resources.getBoolean(R.bool.pref_bell_enabled_default).toString()
            )
            countMode = SharedPreferenceLiveData.of(
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
    
    lateinit var debateBellEnabled: SharedPreferenceLiveData<Boolean>
    
    lateinit var countMode: SharedPreferenceLiveData<CountMode>
}