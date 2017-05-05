package andre.debatetimer

import andre.debatetimer.extensions.DebateBell
import andre.debatetimer.extensions.abs
import android.os.CountDownTimer

/**
 * Created by Andre on 5/4/2017.
 */
abstract class DebateTimer(timerOption: TimerOption) {
	private val countDownTimer: CountDownTimer
	
	init {
		countDownTimer = object : CountDownTimer(Long.MAX_VALUE - 1, 1000) {
			override fun onFinish() {}
			
			override fun onTick(millisUntilFinished: Long) {
				if (!paused) {
					countUpSeconds++
					countDownSeconds--
					
					val absVal = countDownSeconds.abs()
					secondsUntilEnd = absVal % 60
					minutesUntilEnd = absVal / 60
					
					secondsSinceStart = countUpSeconds % 60
					minutesSinceStart = countUpSeconds / 60
					
					bellsSinceStart[countUpSeconds]?.ring()
					
					if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
						DebateBell.TWICE.ring()
						if (countDownSeconds <= -120) {
							stop()
						}
					}
					
					isOvertime = countDownSeconds < 0
					
					onSecond()
				}
			}
		}.start()
	}
	
	private var countUpSeconds: Int = -1
	private var countDownSeconds: Int = timerOption.seconds + 1
	private var paused = true
	
	fun pause() {
		paused = true
	}
	
	fun resume() {
		paused = false
	}
	
	fun stop() {
		pause()
		countDownTimer.cancel()
	}
	
	val bellsSinceStart = timerOption.bellsSinceStart
	
	var secondsUntilEnd = 0
		private set
	var minutesUntilEnd = 0
		private set
	
	var secondsSinceStart = 0
		private set
	var minutesSinceStart = 0
		private set
	
	var isOvertime = false
		private set
	
	abstract fun onSecond()
	
	override fun toString(): String {
		return "DebateTimer"
	}
}