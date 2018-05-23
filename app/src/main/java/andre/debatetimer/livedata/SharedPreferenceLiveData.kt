package andre.debatetimer.livedata

import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

abstract class SharedPreferenceLiveData<T>(private val sp: SharedPreferences,
                                           private val key: String,
                                           private val default: T,
                                           private val getFunction: SharedPreferences.(key: String, default: T) -> T,
                                           private val setFunction: SharedPreferences.Editor.(key: String, value: T) -> SharedPreferences.Editor) : LiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {
	init {
		value = sp.getFunction(key, default)
	}
	
	
	override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
		if (key == this.key) {
			value = sp.getFunction(key, default)
		}
	}
	
	fun apply(value: T) {
		sp.edit().setFunction(key, value).apply()
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
	
	fun observe(owner: LifecycleOwner, observer: (T) -> Unit) {
		val obv = Observer<T> { observer(it!!) }
		super.observe(owner, obv)
	}
	
	companion object {
		fun of(sp: SharedPreferences, key: String, default: Boolean): SharedPreferenceLiveData<Boolean> {
			return object : SharedPreferenceLiveData<Boolean>(sp, key, default, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean) {}
		}
		
		fun of(sp: SharedPreferences, key: String, default: Int): SharedPreferenceLiveData<Int> {
			return object : SharedPreferenceLiveData<Int>(sp, key, default, SharedPreferences::getInt, SharedPreferences.Editor::putInt) {}
		}
		
		fun of(sp: SharedPreferences, key: String, default: String): SharedPreferenceLiveData<String> {
			return object : SharedPreferenceLiveData<String>(sp, key, default, SharedPreferences::getString, SharedPreferences.Editor::putString) {}
		}
		
		fun <T> of(sp: SharedPreferences, key: String, default: String, serialize: (T) -> String, deserialize: (String) -> T): SharedPreferenceLiveData<T> {
			fun SharedPreferences.getFunction(key: String, default: T): T {
				return deserialize(this.getString(key, serialize(default)))
			}
			
			fun SharedPreferences.Editor.setFunction(key: String, value: T): SharedPreferences.Editor {
				putString(key, serialize(value))
				return this
			}
			
			return object : SharedPreferenceLiveData<T>(sp, key, deserialize(default), SharedPreferences::getFunction, SharedPreferences.Editor::setFunction) {}
		}
	}
}