package andre.debatetimer.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

open class NonNullMutableLiveData<T>(init: T) : NonNullLiveData<T>(init) {
    public override fun setValue(value: T) {
        super.setValue(value)
    }
    
    public override fun postValue(value: T) {
        super.postValue(value)
    }
}

open class NonNullLiveData<T>(init: T) : LiveData<T>() {
    init {
        super.setValue(init)
    }
    
    override fun setValue(value: T) {
        super.setValue(value!!)
    }
    
    override fun getValue(): T {
        return super.getValue()!!
    }
    
    override fun postValue(value: T) {
        super.postValue(value!!)
    }
}

typealias MutableBooleanLiveData = NonNullMutableLiveData<Boolean>

typealias BooleanLiveData = NonNullLiveData<Boolean>

typealias MutableStringLiveData = NonNullMutableLiveData<String>

typealias StringLiveData = NonNullLiveData<String>

typealias MutableIntLiveData = NonNullMutableLiveData<Int>

typealias IntLiveData = NonNullLiveData<Int>

fun <T> LiveData<T>.observe(lifecycleOwner: LifecycleOwner, observerFunction: (T?) -> Unit) {
    this.observe(lifecycleOwner, Observer(observerFunction))
}

fun <T> NonNullLiveData<T>.observe(lifecycleOwner: LifecycleOwner, observerFunction: (T) -> Unit) {
    this.observe(lifecycleOwner, Observer(observerFunction))
}