package andre.debatetimer.livedata

import android.annotation.SuppressLint
import android.content.SharedPreferences

abstract class SharedPreferenceLiveData<T>(
        private val sp: SharedPreferences,
        private val key: String,
        private val default: String,
        private val spGetter: (sp: SharedPreferences, key: String, default: String) -> T,
        private val spSetter: (editor: SharedPreferences.Editor, key: String, value: T) -> SharedPreferences.Editor
) : NonNullMutableLiveData<T>(spGetter(sp, key, default)), SharedPreferences.OnSharedPreferenceChangeListener {
    
    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == this.key) {
            super.setValue(spGetter(sp, key, default))
        }
    }
    
    @SuppressLint("ApplySharedPref")
    override fun setValue(value: T) {
        val editor = sp.edit()
        spSetter(editor, key, value)
        editor.commit()
    }
    
    override fun postValue(value: T) {
        val editor = sp.edit()
        spSetter(editor, key, value)
        editor.apply()
    }
    
    override fun onActive() {
        sp.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onInactive() {
        sp.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    companion object {
        fun ofBoolean(
                sp: SharedPreferences,
                key: String,
                default: String
        ): SharedPreferenceLiveData<Boolean> {
            return object : SharedPreferenceLiveData<Boolean>(
                    sp,
                    key,
                    default,
                    { sp, key, default -> sp.getBoolean(key, default.toBoolean()) },
                    { editor, key, value -> editor.putBoolean(key, value) }
            ) {}
        }
    
    
        fun <T> ofString(
                sp: SharedPreferences,
                key: String,
                default: String,
                fromString: (String) -> T,
                toString: (T) -> String
        ): SharedPreferenceLiveData<T> {
            return object : SharedPreferenceLiveData<T>(
                    sp,
                    key,
                    default,
                    { sp, key, default -> fromString(sp.getString(key, default)!!) },
                    { editor, key, value -> editor.putString(key, toString(value)) }
            ) {}
        }
    }
}