package andre.debatetimer.livedata

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferenceLiveData<T>(private val sp: SharedPreferences,
                                           private val key: String,
                                           private val default: String,
                                           private val spGetter: (sp: SharedPreferences, key: String, default: String) -> T,
                                           private val spSetter: (editor: SharedPreferences.Editor, key: String, value: T) -> SharedPreferences.Editor) : LiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {
    init {
        super.setValue(spGetter(sp, key, default)!!)
    }
    
    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == this.key) {
            value = spGetter(sp, key, default)
        }
    }
    
    public override fun setValue(value: T) {
        super.setValue(value)
        val editor = sp.edit()
        spSetter(editor, key, value)
        editor.apply()
    }
    
    override fun getValue(): T {
        return super.getValue()!!
    }
    
    override fun onActive() {
        sp.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onInactive() {
        sp.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    companion object {
        fun ofBoolean(sp: SharedPreferences, key: String, default: String): SharedPreferenceLiveData<Boolean> =
                object : SharedPreferenceLiveData<Boolean>(
                        sp,
                        key,
                        default,
                        { sp, key, default -> sp.getBoolean(key, default.toBoolean()) },
                        { editor, key, value -> editor.putBoolean(key, value) }) {}
    
    
        fun <T> of(sp: SharedPreferences,
                   key: String,
                   default: String,
                   fromString: (String) -> T,
                   toString: (T) -> String) =
                object : SharedPreferenceLiveData<T>(
                        sp,
                        key,
                        default,
                        { sp, key, default -> fromString(sp.getString(key, default)!!) },
                        { editor, key, value -> editor.putString(key, toString(value)) }
                ) {}
    }
}