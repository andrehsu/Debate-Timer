package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.content.Context
import android.view.View

interface IMainPresenter {
	var view: IMainView
	
	var timerMaps: Map<Int, TimerOption>
	
	var state: State
	
	fun newTimerInstance(presenter: IMainPresenter, timerOption: TimerOption): DebateTimer
	
	fun onStartPause(view: View)
	
	fun onTimeButtonSelect(view: View)
	
	fun onToggleDisplayMode()
	
	fun onDestroy(context: Context)
	fun subscribe()
}