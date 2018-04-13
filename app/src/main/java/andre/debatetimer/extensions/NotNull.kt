package andre.debatetimer.extensions

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


open class CustomNotNullVar<T : Any> : ReadWriteProperty<Any?, T> {
	private var _value: T? = null
	
	var field: T
		get() {
			return _value
					?: throw IllegalStateException("Property should be initialized before get.")
		}
		set(value) {
			this._value = value
		}
	
	final override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return this.getter()
	}
	
	final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		this.setter(value)
	}
	
	open fun CustomNotNullVar<T>.getter(): T {
		return field
	}
	
	open fun CustomNotNullVar<T>.setter(value: T) {
		this.field = value
	}
}