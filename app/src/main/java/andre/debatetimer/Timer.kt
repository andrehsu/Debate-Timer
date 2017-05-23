package andre.debatetimer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.os.SystemClock

/**
 * Schedule a countdown until a time in the future, with
 * regular notifications on intervals along the way.
 
 * Example of showing a 30 second countdown in a text field:
 
 * <pre class="prettyprint">
 * new Timer(30000, 1000) {
 
 * public void onTick(long millisUntilFinished) {
 * mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
 * }
 
 * public void onFinish() {
 * mTextField.setText("done!");
 * }
 * }.start();
</pre> *
 
 * The calls to [.onTick] are synchronized to this object so that
 * one call to [.onTick] won't ever occur before the previous
 * callback is complete.  This is only relevant when the implementation of
 * [.onTick] takes an amount of time to execute that is significant
 * compared to the countdown interval.
 */
abstract class Timer
/**
 * @param timerInterval The interval along the way to receive
 * *   [.onTick] callbacks.
 */
constructor(
		/**
		 * The interval in millis that the user receives callbacks
		 */
		private val timerInterval: Long
) {
	private var firstTickPast = false
	
	/**
	 * boolean representing if the timer was cancelled
	 */
	private var cancelled = false
	
	/**
	 * Cancel the countdown.
	 */
	@Synchronized fun cancel() {
		firstTickPast = false
		cancelled = true
		handler.removeMessages(MSG)
	}
	
	/**
	 * Start the countdown.
	 */
	@Synchronized fun start(): Timer {
		cancelled = false
		handler.sendMessage(handler.obtainMessage(MSG))
		return this
	}
	
	/**
	 * Callback fired on regular interval.
	 */
	abstract fun onTick()
	
	open fun onTickAfterStart() {}
	
	// handles timing
	private val handler = @SuppressLint("HandlerLeak")
	object : Handler() {
		override fun handleMessage(msg: Message) {
			synchronized(this@Timer) {
				if (cancelled) {
					return
				}
				
				val lastTickStart = SystemClock.elapsedRealtime()
				if (firstTickPast)
					onTick()
				else {
					onTickAfterStart()
					firstTickPast = true
				}
				
				// take into account user's onTick taking time to execute
				var delay = lastTickStart + timerInterval - SystemClock.elapsedRealtime()
				
				// special case: user's onTick took more than interval to
				// complete, skip to next interval
				while (delay < 0) delay += timerInterval
				
				sendMessageDelayed(obtainMessage(MSG), delay)
			}
		}
	}
	
	companion object {
		private const val MSG = 1
	}
}
