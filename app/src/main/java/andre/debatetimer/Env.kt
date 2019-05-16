package andre.debatetimer

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.absoluteValue

object EnvVars {
    private val LogTag = EnvVars::class.java.simpleName
    
    private inline fun <R> requireInitialized(block: () -> R): R {
        require(initialized) { "EnvVars not initialized" }
        return block()
    }
    
    private var initialized = false
    
    var color_timerStart: Int = -1
        get () = requireInitialized { return field }
        private set
    var color_timerNormal: Int = -1
        get () = requireInitialized { return field }
        private set
    var color_timerEnd: Int = -1
        get () = requireInitialized { return field }
        private set
    var color_timerOvertime: Int = -1
        get () = requireInitialized { return field }
        private set
    
    
    fun init(context: Context) {
        if (!initialized) {
            with(context) {
                color_timerStart = getColorCompat(R.color.timerStart)
                color_timerNormal = getColorCompat(R.color.timerNormal)
                color_timerEnd = getColorCompat(R.color.timerEnd)
                color_timerOvertime = getColorCompat(R.color.timerOvertime)
            }
            
            initialized = true
        } else {
            Log.d(LogTag, "Already initialized")
        }
    }
}

fun secondsToString(seconds: Int): String {
    val abs = seconds.absoluteValue
    val minutes = abs / 60
    val secondsOnly = abs % 60
    return if (seconds >= 0) {
        ""
    } else {
        "-"
    } + "$minutes:${secondsOnly.toString().padStart(2, '0')}"
}

fun Context.getColorCompat(id: Int) = ContextCompat.getColor(this, id)