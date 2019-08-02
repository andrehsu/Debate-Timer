package andre.debatetimer.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

fun <X, Y> LiveData<X>.map(mapFunction: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, mapFunction)
}

fun <X, Y> LiveData<X>.switchMap(switchMapFunction: (X) -> LiveData<Y>): LiveData<Y> {
    return Transformations.switchMap(this, switchMapFunction)
}

@Suppress("FunctionName")
fun <T> MutableLiveData(value: T): MutableLiveData<T> {
    val mutableLiveData = MutableLiveData<T>()
    mutableLiveData.value = value
    return mutableLiveData
}

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observeFunc: (T) -> Unit) {
    this.observe(owner, observeFunc)
}