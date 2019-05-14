package andre.debatetimer

import andre.debatetimer.extensions.defaultSharedPreferences
import andre.debatetimer.livedata.SharedPreferenceLiveData
import android.content.Context
import android.util.Log

object Prefs {
    private val LogTag = Prefs::class.java.simpleName
    
    private var initialized = false
    
    lateinit var pref_bell_enabled_key: String
        private set
    
    lateinit var pref_count_mode: String
        private set
    
    fun init(context: Context) {
        if (!initialized) {
            var pref_bell_enabled_default: Boolean
            var pref_count_mode_default: String
            with(context.resources) {
                pref_bell_enabled_key = getString(R.string.pref_bell_enabled_key)
                pref_bell_enabled_default = getBoolean(R.bool.pref_bell_enabled_default)
                pref_count_mode = getString(R.string.pref_count_mode)
                pref_count_mode_default = getString(R.string.pref_count_mode_default)
            }
            
            val sp = context.defaultSharedPreferences
            debateBellEnabled = SharedPreferenceLiveData.of(sp, pref_bell_enabled_key, pref_bell_enabled_default)
            countMode = SharedPreferenceLiveData.of(sp, pref_count_mode, pref_count_mode_default, CountMode::toString, CountMode.Companion::fromString)
            
            initialized = true
        } else {
            Log.d(LogTag, "Already initialized")
        }
    }
    
    lateinit var debateBellEnabled: SharedPreferenceLiveData<Boolean>
    
    lateinit var countMode: SharedPreferenceLiveData<CountMode>
}