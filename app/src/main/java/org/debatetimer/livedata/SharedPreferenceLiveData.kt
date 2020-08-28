package org.debatetimer.livedata

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

fun SharedPreferences.getBooleanLiveData(key: String, default: Boolean): SharedPreferenceLiveData<Boolean> {
    return object : SharedPreferenceLiveData<Boolean>(this, key, default) {
        override fun getFromSp(sp: SharedPreferences, key: String, default: Boolean): Boolean {
            return sp.getBoolean(key, default)
        }
        
        override fun putToSp(editor: SharedPreferences.Editor, key: String, value: Boolean) {
            editor.putBoolean(key, value)
        }
    }
}

fun SharedPreferences.getStringLiveData(key: String, default: String): SharedPreferenceLiveData<String> {
    return object : SharedPreferenceLiveData<String>(this, key, default) {
        override fun getFromSp(sp: SharedPreferences, key: String, default: String): String {
            return sp.getString(key, default)!!
        }
        
        override fun putToSp(editor: SharedPreferences.Editor, key: String, value: String) {
            editor.putString(key, value)
        }
    }
}

fun <T> SharedPreferences.getObjectLiveData(key: String, defaultObjectAsString: String, objectFromString: (String) -> T, objectToString: (T) -> String): SharedPreferenceLiveData<T> {
    return object : SharedPreferenceLiveData<T>(this, key, objectFromString(defaultObjectAsString)) {
        override fun getFromSp(sp: SharedPreferences, key: String, default: T): T {
            return objectFromString(sp.getString(key, defaultObjectAsString)!!)
        }
        
        override fun putToSp(editor: SharedPreferences.Editor, key: String, value: T) {
            editor.putString(key, objectToString(value))
        }
    }
}

abstract class SharedPreferenceLiveData<T>(
        private val sp: SharedPreferences,
        private val key: String,
        private val default: T
) : LiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {
    init {
        super.setValue(getFromSp(sp, key, default))
        sp.registerOnSharedPreferenceChangeListener(this)
    }
    
    abstract fun getFromSp(sp: SharedPreferences, key: String, default: T): T
    
    abstract fun putToSp(editor: SharedPreferences.Editor, key: String, value: T)
    
    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == this.key) {
            super.setValue(getFromSp(sp, key, default))
        }
    }
    
    override fun getValue(): T {
        return super.getValue()!!
    }
    
    public override fun setValue(value: T) {
        val editor = sp.edit()
        putToSp(editor, key, value)
        editor.apply()
    }
}