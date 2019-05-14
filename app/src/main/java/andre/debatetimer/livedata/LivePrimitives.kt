package andre.debatetimer.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

abstract class NLiveData<T>(init: T) : MutableLiveData<T>() {
    init {
        value = init
    }
    
    final override fun postValue(value: T) {
        super.postValue(value)
    }
    
    final override fun setValue(value: T) {
        super.setValue(value)
    }
    
    final override fun getValue(): T {
        return super.getValue()!!
    }
    
    fun observe(owner: LifecycleOwner, observer: (T) -> Unit) {
        val obv = Observer<T> { observer(it!!) }
        super.observe(owner, obv)
    }
    
    override fun toString(): String {
        return value.toString()
    }
}

class BooleanLiveData(boolean: Boolean = false) : NLiveData<Boolean>(boolean)

class IntLiveData(int: Int = 0) : NLiveData<Int>(int)

class StringLiveData(string: String = "") : NLiveData<String>(string)