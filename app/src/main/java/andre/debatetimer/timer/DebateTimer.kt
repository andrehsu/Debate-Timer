package andre.debatetimer.timer

import andre.debatetimer.EnvVars
import andre.debatetimer.extensions.abs
import android.support.annotation.ColorInt
import android.util.Log

abstract class DebateTimer(timerOption: TimerOption) {
	private var countUpSeconds: Int = 0
	private var countDownSeconds: Int = timerOption.seconds
	private var timer: Timer = newTimerInstance()
	private var deciseconds: Int = 0
	
	fun newTimerInstance() = object : Timer(100) {
		override fun onTick() {
			deciseconds++
			if (deciseconds == 10) {
				deciseconds = 0
				onSecondInternal()
			}
		}
	}
	
	private fun onSecondInternal() {
		if (countDownSeconds <= -60) {
			pause()
			onEnd()
			return
		}
		
		Log.d("DebateTimer", "tick")
		
		countUpSeconds++
		countDownSeconds--
		
		val absVal = countDownSeconds.abs()
		
		isTimeEndNegative = countDownSeconds < 0
		secondsLeft = absVal % 60
		minutesLeft = absVal / 60
		
		secondsSinceStart = countUpSeconds % 60
		minutesSinceStart = countUpSeconds / 60
		
		bellsSinceStart[countUpSeconds]?.let { onBell(it) }
		
		if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
			onBell(DebateBell.Twice)
		}
		
		if (countUpSeconds == 60 && countDownSeconds > 60) {
			textColor = EnvVars.color_timerNormal
			onFirstMinuteEnd()
		}
		
		if (countDownSeconds == 60) {
			textColor = EnvVars.color_timerEnd
			onLastMinuteStart()
		}
		
		if (countDownSeconds == -1) {
			textColor = EnvVars.color_timerEnd
			onOvertime()
		}
		
		onSecond()
	}
	
	fun pause() {
		timer.cancel()
		timer = newTimerInstance()
	}
	
	fun resume() {
		timer.start()
	}
	
	val bellsSinceStart = timerOption.bellsSinceStart
	
	var isTimeEndNegative = false
		private set
	var secondsLeft = 0
		private set
	var minutesLeft = 0
		private set
	
	var secondsSinceStart = 0
		private set
	var minutesSinceStart = 0
		private set
	
	@ColorInt
	var textColor: Int = EnvVars.color_timerStart
		private set
	
	abstract fun onSecond()
	
	open fun onFirstMinuteEnd() {}
	
	open fun onLastMinuteStart() {}
	
	open fun onOvertime() {}
	
	open fun onEnd() {}
	
	open fun onBell(debateBell: DebateBell) {}
	
	override fun toString(): String = "DebateTimer"
}

enum class DebateBell {
	Once, Twice
}