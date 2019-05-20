package andre.debatetimer

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Res {
    private val LogTag = Res::class.java.simpleName
    
    private var initialized = false
    
    private class Property<T>(init: T) : ReadWriteProperty<Any, T> {
        private var value: T = init
        
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            require(initialized) { "Res not initialized" }
            return value
        }
        
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            require(!initialized) { "Res already initialized" }
            this.value = value
        }
    }
    
    private val StringProperty
        get() = Property("")
    private val IntProperty
        get() = Property(0)
    
    @Suppress("ClassName")
    object color {
        var timerStart by IntProperty
        var timerNormal by IntProperty
        var timerEnd by IntProperty
        var timerOvertime by IntProperty
    }
    
    @Suppress("ClassName")
    object string {
        var on by StringProperty
        var off by StringProperty
        
        var start by StringProperty
        var pause by StringProperty
        var resume by StringProperty
        
        var overtimeBy by StringProperty
    }
    
    fun init(context: Context) {
        if (!initialized) {
            with(context) {
                color.timerStart = getColorCompat(R.color.timerStart)
                color.timerNormal = getColorCompat(R.color.timerNormal)
                color.timerEnd = getColorCompat(R.color.timerEnd)
                color.timerOvertime = getColorCompat(R.color.timerOvertime)
                
                string.on = getString(R.string.on)
                string.off = getString(R.string.off)
                string.overtimeBy = resources.getString(R.string.overtime)
                string.start = getString(R.string.start)
                string.pause = getString(R.string.pause)
                string.resume = getString(R.string.resume)
            }
            
            initialized = true
        } else {
            Log.d(LogTag, "Already initialized")
        }
    }
}


private fun Context.getColorCompat(id: Int) = ContextCompat.getColor(this, id)