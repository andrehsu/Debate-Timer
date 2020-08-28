package org.debatetimer.livedata

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

fun SharedPreferences.getBooleanLiveData(key: String, default: Boolean): SharedPreferenceLiveData<Boolean> {
    return SharedPreferenceLiveData(
            this,
            key,
            { getBoolean(key, default) },
            { putBoolean(key, it) }
    )
}

fun SharedPreferences.getStringLiveData(key: String, default: String): SharedPreferenceLiveData<String> {
    return SharedPreferenceLiveData(
            this,
            key,
            { getString(key, default)!! },
            { putString(key, it) }
    )
}

fun <T> SharedPreferences.getObjectLiveData(key: String, defaultObjectAsString: String, objectFromString: (String) -> T, objectToString: (T) -> String): SharedPreferenceLiveData<T> {
    return SharedPreferenceLiveData(
            this,
            key,
            { objectFromString(getString(key, defaultObjectAsString)!!) },
            { putString(key, objectToString(it)) }
    )
}

class SharedPreferenceLiveData<T>(
        private val sp: SharedPreferences,
        private val key: String,
        private val get: SharedPreferences.() -> T,
        private val put: SharedPreferences.Editor.(value: T) -> Unit
) : LiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {
    init {
        super.setValue(sp.get())
        super.setValue(sp.get())
        sp.registerOnSharedPreferenceChangeListener(this)
    }
    
    
    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == this.key) {
            super.setValue(get(sp))
        }
    }
    
    override fun getValue(): T {
        return super.getValue()!!
    }
    
    public override fun setValue(value: T) {
        val editor = sp.edit()
        editor.put(value)
        editor.apply()
    }
}