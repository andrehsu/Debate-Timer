package andre.debatetimer

import android.content.Context
import android.widget.Button

interface IMainView {
	var presenter: IMainPresenter
	
	var timerTextColor: Int
	
	var timerMinutes: Int
	
	var timerSeconds: Int
	
	var timerCountMode: CountMode
	
	var buttonsActive: Boolean
	
	var keepScreenOn: Boolean
	
	var tv_startPauseText: String
	
	
	val context: Context
	
	fun updateDebateBellIcon()
	
	fun updateTimerValue()
	
	fun updateTimerColor()
	
	fun updateTimerBinding()
	
	fun setBegan()
	
	fun updateBells()
	
	fun updateCountModeTitle()
	
	fun resetBegan()
	
	fun crossfadeStartPause()
	
	fun setTimeButtonAsSelected(view: Button)
}