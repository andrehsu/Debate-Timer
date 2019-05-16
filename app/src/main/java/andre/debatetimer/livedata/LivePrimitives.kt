package andre.debatetimer.livedata

import andre.debatetimer.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

open class NonNullLiveData<T>(init: T) : MutableLiveData<T>() {
    init {
        super.setValue(init)
    }
    
    override fun getValue(): T {
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

typealias BooleanLiveData = NonNullLiveData<Boolean>

typealias StringLiveData = NonNullLiveData<String>

typealias IntLiveData = NonNullLiveData<Int>

typealias StateLiveData = NonNullLiveData<State>