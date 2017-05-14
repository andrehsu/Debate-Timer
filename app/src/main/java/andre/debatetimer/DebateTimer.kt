package andre.debatetimer

import andre.debatetimer.extensions.DebateBell
import andre.debatetimer.extensions.abs

/**
 * Created by Andre on 5/4/2017.
 */
abstract class DebateTimer(timerOption: TimerOption) {
	private var countUpSeconds: Int = 0
	private var countDownSeconds: Int = timerOption.seconds
	private var timer: Timer = newTimerInstance()
	
	fun newTimerInstance() = object : Timer(1000) {
		override fun onTick() {
			if (countDownSeconds <= -120) {
				stop()
				return
			}
			
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
			}
			
			if (countUpSeconds == 60) {
				onFirstMinuteEnd()
			}
			
			if (countDownSeconds == 60) {
				onLastMinuteStart()
			}
			
			if (countDownSeconds == -1) {
				onOvertime()
			}
			
			isOvertime = countDownSeconds < 0
			
			onSecond()
		}
	}
	
	fun pause() {
		timer.cancel()
		timer = newTimerInstance()
	}
	
	fun resume() {
		timer.start()
	}
	
	fun stop() {
		timer.cancel()
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
	
	open fun onFirstMinuteEnd() {}
	
	open fun onLastMinuteStart() {}
	
	open fun onOvertime() {}
	
	override fun toString(): String {
		return "DebateTimer"
	}
}