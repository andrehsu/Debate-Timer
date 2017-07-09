package andre.debatetimer

import android.view.View

interface TimerView {
	var timerMinutes: Int
	
	var timerSeconds: Int
	
	var timerIsNegative: Boolean
	
	var timerTextColor: Int
	
	var timerDisplayMode: TimerDisplayMode
	
	var buttonsActive: Boolean
	
	var keepScreenOn: Boolean
	
	fun refreshBells()
	
	fun refreshTimer()
	
	fun onFirstTimeButtonSelect()
	
	fun onStartPause(view: View)
	
	fun onToggleDisplayMode(view: View)
	
	fun onTimeButtonSelect(view: View)
}