package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.view.View

interface IMainPresenter {
	var view: IMainView
	
	var timerMaps: Map<String, TimerOption>
	
	var state: State
	
	fun newTimerInstance(presenter: IMainPresenter, timerOption: TimerOption): DebateTimer
	
	fun onStartPause(view: View)
	
	fun onTimeButtonSelect(view: View)
	
	fun onToggleDisplayMode()
	
	fun subscribe()
}