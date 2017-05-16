package andre.debatetimer

import andre.debatetimer.DebateBell.Companion.debateBellEnabled
import andre.debatetimer.extensions.*
import andre.debatetimer.extensions.EnvVars.color_timerEnd
import andre.debatetimer.extensions.EnvVars.color_timerNormal
import andre.debatetimer.extensions.EnvVars.color_timerStart
import andre.debatetimer.extensions.EnvVars.init
import andre.debatetimer.extensions.EnvVars.longAnimTime
import android.app.Dialog
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.forEachChild
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
	//<editor-fold desc="State classes">
	interface State
	
	interface HasTimerOption : State {
		val timerOption: TimerOption
	}
	
	object WaitingToBegin : State
	
	data class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption
	
	inner class TimerStarted(override val timerOption: TimerOption, val timer: DebateTimer) : State, HasTimerOption {
		var running: Boolean by Delegates.observable(false) { _, _, newValue ->
			if (newValue) {
				timer.resume()
				window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				buttons.forEach {
					it.isClickable = false
					it.alpha = 0.54f
				}
			} else {
				timer.pause()
				window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				buttons.forEach {
					it.isClickable = true
					it.alpha = 1.0f
				}
			}
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="Fields">
	private lateinit var buttons: List<Button>
	private lateinit var timerTexts: List<TextView>
	private lateinit var action_debateBell: MenuItem
	private var timerDisplayCountUp = false
	private var state: State by Delegates.observable(WaitingToBegin as State) { _, oldValue, newValue ->
		require(newValue !is WaitingToBegin)
		
		if (oldValue is WaitingToBegin) {
			cl_timer.setVisible()
			bt_startPause.setVisible()
			action_debateBell.isVisible = true
			tv_startingText.setGone()
		}
		
		if (oldValue is TimerStarted && (newValue !is TimerStarted || newValue is TimerStarted && oldValue.timer !== newValue.timer)) {
			oldValue.timer.stop()
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="Activity callbacks">
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		//<editor-fold desc="Field initialization">
		init(this)
		
		timerTexts = listOf(tv_timerNegative, tv_timer_m, tv_timer_s, tv_timer_colon)
		buttons = mutableListOf()
		ll_timeButtons.forEachChild { child ->
			child as Button
			
			buttons += child
			
			val tag = child.tag as String
			
			TimerOption.parseTag(tag)
			
			child.text = if (tag.isNotBlank()) {
				val seconds = tag.substringBefore(';').toInt()
				val minuteOnly = seconds / 60
				val secondsOnly = seconds % 60
				
				val sb = StringBuilder()
				if (minuteOnly != 0) {
					sb.append(minuteOnly).append('m').append(' ')
				}
				if (secondsOnly != 0) {
					sb.append(secondsOnly).append("s")
				}
				
				sb.toString().trim()
			} else {
				"???"
			}
		}
		//</editor-fold>
		
		//<editor-fold desc="Show debug button if debugging">
		if (BuildConfig.DEBUG) {
			bt_debug.setVisible()
		}
		//</editor-fold>
		
		//<editor-fold desc="StartPause button onClick">
		bt_startPause.setOnClickListener {
			bt_startPause crossfadeTo bt_startPause withDuration longAnimTime
			val state = state
			when (state) {
				is WaitingToStart -> {
					val timer = object : DebateTimer(state.timerOption) {
						override fun onSecond() = refreshTimer()
						
						override fun onFirstMinuteEnd() {
							ui_color = color_timerNormal
						}
						
						override fun onLastMinuteStart() {
							ui_color = color_timerEnd
						}
						
						override fun onOvertime() {
							ui_color = color_timerEnd
						}
					}
					
					this.state = TimerStarted(state.timerOption, timer)
					
					bt_startPause.callOnClick()
				}
				is TimerStarted -> {
					if (state.running) {
						bt_startPause.text = getString(R.string.resume)
						state.running = false
					} else {
						bt_startPause.text = getString(R.string.pause)
						state.running = true
					}
				}
			}
		}
		//</editor-fold>
		
		//<editor-fold desc="Time buttons onClick">
		buttons.forEach {
			it.setOnClickListener { view ->
				buttons.forEach {
					it.setTextColor(getColorCompat(R.color.buttonUnselected))
				}
				
				view as Button
				view.setTextColor(getColorCompat(R.color.buttonSelected))
				
				bt_startPause.text = getString(R.string.start)
				
				val timerOption = TimerOption.parseTag(view.tag as String)
				
				val state = state
				if (state is TimerStarted) {
					state.running = false
				}
				
				this.state = WaitingToStart(timerOption)
				
				refreshTimer()
				refreshBellLabel()
			}
		}
		//</editor-fold>
		
		//<editor-fold desc="Set on toggle for elapsed/ remaining">
		cl_timer.setOnClickListener {
			timerDisplayCountUp = !timerDisplayCountUp
			if (timerDisplayCountUp) {
				tv_timerDisplayMode.text = getString(R.string.timer_display_count_up)
			} else {
				tv_timerDisplayMode.text = getString(R.string.timer_display_count_down)
			}
			refreshTimer()
			refreshBellLabel()
		}
		//</editor-fold>
	}
	
	class ExitDialogFragment : DialogFragment() {
		override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
			return AlertDialog.Builder(activity)
					.setTitle(R.string.exit_question)
					.setPositiveButton(android.R.string.yes, { _, _ -> activity.finish() })
					.setNegativeButton(android.R.string.no, { _, _ -> dialog.cancel() })
					.create()
		}
	}
	
	override fun onBackPressed() {
		if (isTaskRoot) {
			ExitDialogFragment().show(supportFragmentManager, null)
		} else {
			super.onBackPressed()
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		action_debateBell = menu.findItem(R.id.action_debate_bell)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_debate_bell -> {
				debateBellEnabled = !debateBellEnabled
				if (debateBellEnabled) {
					action_debateBell.icon = getDrawable(R.drawable.ic_notifications_active_white_24dp)
				} else {
					action_debateBell.icon = getDrawable(R.drawable.ic_notifications_off_white_24dp)
				}
				refreshBellLabel()
			}
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}
	//</editor-fold>
	
	//<editor-fold desc="UI fields">
	private var ui_minutes: Int by Delegates.observable(0) { _, oldValue, minutes ->
		if (oldValue != minutes) {
			tv_timer_m.text = minutes.toString()
		}
	}
	private var ui_seconds: Int by Delegates.observable(0) { _, oldValue, seconds ->
		if (oldValue != seconds) {
			tv_timer_s.text = seconds.toString().padStart(2, '0')
		}
	}
	private var ui_isNegative: Boolean by Delegates.observable(false) { _, oldValue, isNegative ->
		if (oldValue != isNegative) {
			if (isNegative) {
				tv_timerNegative.setVisible()
				guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).also { it.guidePercent = 0.5f }
			} else {
				tv_timerNegative.setGone()
				guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).also { it.guidePercent = 0.44f }
			}
		}
	}
	private var ui_color: Int by Delegates.observable(color_timerStart) { _, oldValue, newValue ->
		if (oldValue != newValue) {
			timerTexts.forEach { it.setTextColor(newValue) }
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="UI functions">
	private fun refreshTimer() {
		val state = state
		if (state is WaitingToStart) {
			if (timerDisplayCountUp) {
				ui_minutes = 0
				ui_seconds = 0
			} else {
				ui_minutes = state.timerOption.minutesOnly
				ui_seconds = state.timerOption.secondsOnly
			}
			ui_isNegative = false
			ui_color = color_timerStart
		} else if (state is TimerStarted) {
			val timer = state.timer
			if (timerDisplayCountUp) {
				ui_minutes = timer.minutesSinceStart
				ui_seconds = timer.secondsSinceStart
				ui_isNegative = false
			} else {
				ui_minutes = timer.minutesLeft
				ui_seconds = timer.secondsLeft
				ui_isNegative = timer.isTimeEndNegative
			}
		}
	}
	
	private fun refreshBellLabel() {
		val state = state
		if (debateBellEnabled && state is HasTimerOption) {
			val timerOption = state.timerOption
			if (timerOption.countUpString.isEmpty()) {
				tv_bellsAt.setInvisible()
			} else {
				tv_bellsAt.setVisible()
				
				val format = if (timerOption.bellsSinceStart.count() == 1) {
					getString(R.string.bell_at)
				} else {
					getString(R.string.bells_at)
				}
				
				val string = if (timerDisplayCountUp) {
					timerOption.countUpString
				} else {
					timerOption.countDownString
				}
				
				tv_bellsAt.text = format.format(string)
			}
		} else {
			tv_bellsAt.setInvisible()
		}
	}
	//</editor-fold>
}