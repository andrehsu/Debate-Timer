package andre.debatetimer.timer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.os.SystemClock

abstract class Timer
constructor(private val timerInterval: Long) {
    private var cancelled = false
    @Synchronized
    fun cancel() {
        cancelled = true
        handler.removeMessages(MSG)
    }
    
    @Synchronized
    fun start(): Timer {
        cancelled = false
        handler.sendMessage(handler.obtainMessage(MSG))
        return this
    }
    
    abstract fun onTick()
    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            if (cancelled) {
                return
            }
            val lastTickStart = SystemClock.elapsedRealtime()
            onTick()
            
            // take into account user's onTick taking time to execute
            var delay = lastTickStart + timerInterval - SystemClock.elapsedRealtime()
            
            // special case: user's onTick took more than interval to
            // complete, skip to next interval
            while (delay < 0) delay += timerInterval
            
            sendMessageDelayed(obtainMessage(MSG), delay)
        }
    }
    
    companion object {
        private const val MSG = 1
    }
}
