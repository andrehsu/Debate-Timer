package andre.debatetimer.timer

import andre.debatetimer.extensions.abs

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
			return
		}
		
		countUpSeconds++
		countDownSeconds--
		
		val absVal = countDownSeconds.abs()
		
		isTimeEndNegative = countDownSeconds < 0
		secondsLeft = absVal % 60
		minutesLeft = absVal / 60
		
		secondsSinceStart = countUpSeconds % 60
		minutesSinceStart = countUpSeconds / 60
		
		bellsSinceStart[countUpSeconds]?.ring()
		
		if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
			DebateBell.TWICE.ring()
		}
		
		if (countUpSeconds == 60 && countDownSeconds > 60) {
			onFirstMinuteEnd()
		}
		
		if (countDownSeconds == 60) {
			onLastMinuteStart()
		}
		
		if (countDownSeconds == -1) {
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
	
	abstract fun onSecond()
	
	open fun onFirstMinuteEnd() {}
	
	open fun onLastMinuteStart() {}
	
	open fun onOvertime() {}
	
	override fun toString(): String = "DebateTimer"
}