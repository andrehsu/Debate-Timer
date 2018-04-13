package andre.debatetimer

import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.extensions.*
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : IMainView, AppCompatActivity() {
	private lateinit var retainedFragment: RetainedFragment
	
	override var presenter: IMainPresenter
		get() = retainedFragment.presenter
		set(value) {
			retainedFragment.presenter = value
		}
	
	private lateinit var timerBindings: Map<TimerDisplayMode, TimerBinding>
	private lateinit var timerButtons: List<Button>
	private var action_debateBell: MenuItem? = null
	private var action_countMode: MenuItem? = null
	
	override var timerMinutes: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				timerBinding.minutes = value
			}
		}
	override var timerSeconds: Int = 0
		set(value) {
			if (field != value) {
				field = value
				timerBinding.seconds = value
			}
		}
	override var timerTextColor: Int = -1
		set(value) {
			if (field != value) {
				field = value
				timerBinding.color = field
			}
		}
	override var timerCountMode: CountMode = CountUp
		set(value) {
			field = value
			
			tv_timerCountMode.text = getString(
					if (timerCountMode == CountUp) R.string.timer_display_count_up else R.string.timer_display_count_down
			)
			
			updateTimerValue()
			updateBells()
			updateTimerBinding()
		}
	private lateinit var timerBinding: TimerBinding
	override var buttonsActive: Boolean = false
		set(value) {
			field = value
			if (value) {
				timerButtons.forEach {
					it.isClickable = false
					it.alpha = 0.54f
				}
			} else {
				timerButtons.forEach {
					it.isClickable = true
					it.alpha = 1.0f
				}
			}
		}
	override var keepScreenOn: Boolean = false
		set(value) {
			field = value
			if (value) {
				window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			} else {
				window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}
		}
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		val fm = fragmentManager
		var retainedFragment = fm.findFragmentByTag(RetainedFragment) as? RetainedFragment
		
		if (retainedFragment == null) {
			retainedFragment = RetainedFragment()
			
			this.retainedFragment = retainedFragment
			
			retainedFragment.presenter = MainPresenter(this)
			
			fm.beginTransaction().add(retainedFragment, RetainedFragment).commit()
			
			presenter.state = WaitingToBegin
		} else {
			this.retainedFragment = retainedFragment
			presenter.view = this
		}
		
		fl_timer.setOnClickListener(presenter::onStartPause)
		
		val sp = defaultSharedPreferences
		
		val timersStr = sp.getStringSet(getString(R.string.pref_timers), mutableSetOf(
				"180;-1",
				"240;-1",
				"300;-1",
				"360;-1",
				"420;-1",
				"480;-1")).toList().sorted()
		
		val timerMaps = mutableMapOf<Int, TimerOption>()
		val timerButtons = mutableListOf<Button>()
		
		timersStr.forEach { str ->
			val timerOption = TimerOption.parseTag(str)
			
			if (timerOption != null) {
				val layout = layoutInflater.inflate(R.layout.timer_button, ll_timeButtons, false) as Button
				
				layout.text = with(timerOption) {
					if (minutesOnly != 0 && secondsOnly != 0) {
						"${minutesOnly}m${secondsOnly}s"
					} else if (minutesOnly != 0) {
						"${minutesOnly}m"
					} else if (secondsOnly != 0) {
						"${secondsOnly}s"
					} else {
						"nil"
					}
				}
				layout.setTextColor(getColorCompat(R.color.buttonUnselected))
				layout.setOnClickListener(::onTimeButtonSelect)
				ll_timeButtons.addView(layout)
				timerMaps[layout.id] = timerOption
				timerButtons += layout
			}
		}
		
		presenter.timerMaps = timerMaps
		
		timerBindings = getBindings(this)
		updateTimerBinding()
		
		this.timerButtons = timerButtons
		
		updateTimerValue()
		updateTimerColor()
		volumeControlStream = AudioManager.STREAM_MUSIC
		
		presenter.subscribe()
	}
	
	private fun onTimeButtonSelect(v: View) {
		v as Button
		presenter.onTimeButtonSelect(v)
		setTimeButtonAsSelected(v)
	}
	
	override fun onBackPressed() {
		class ExitDialogFragment : DialogFragment() {
			override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
				return AlertDialog.Builder(this@MainActivity)
						.setTitle(R.string.exit_question)
						.setPositiveButton(android.R.string.yes, { _, _ -> this@MainActivity.finish() })
						.setNegativeButton(android.R.string.no, { _, _ -> dialog.cancel() })
						.create()
			}
		}
		
		if (isTaskRoot) {
			ExitDialogFragment().show(supportFragmentManager, null)
		} else {
			super.onBackPressed()
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		action_debateBell = menu.findItem(R.id.action_debate_bell)
		action_countMode = menu.findItem(R.id.action_count_mode)
		
		updateDebateBellIcon()
		updateCountModeTitle()
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_debate_bell -> {
				Prefs.debateBellEnabled = !Prefs.debateBellEnabled
			}
			R.id.action_count_mode -> {
				presenter.onToggleDisplayMode()
			}
			else -> return super.onOptionsItemSelected(item)
		}
		
		return true
	}
	
	override fun updateDebateBellIcon() {
		action_debateBell?.icon = getDrawable(
				if (Prefs.debateBellEnabled) {
					R.drawable.ic_notifications_active_white_24dp
				} else {
					R.drawable.ic_notifications_off_white_24dp
				}
		)
	}
	
	override fun updateCountModeTitle() {
		action_countMode?.title = getString(
				if (Prefs.countMode == CountUp) {
					R.string.show_time_remaining
				} else {
					R.string.show_time_elapsed
				}
		)
	}
	
	override fun updateTimerBinding() {
		val state = presenter.state
		timerBinding = when {
			state is TimerStarted && state.ended -> timerBindings.getValue(TimerDisplayMode.End)
			state is TimerStarted && state.timer.isTimeEndNegative -> timerBindings.getValue(TimerDisplayMode.Negative)
			state === WaitingToBegin -> NullBinding
			else -> timerBindings.getValue(TimerDisplayMode.Normal)
		}
		for (timerBinding in timerBindings.values) {
			timerBinding.isVisible = timerBinding.timerDisplayMode == this.timerBinding.timerDisplayMode
		}
	}
	
	override fun updateTimerValue() {
		val state = presenter.state
		updateTimerBinding()
		when (state) {
			is WaitingToStart -> {
				if (timerCountMode == CountUp) {
					timerMinutes = 0
					timerSeconds = 0
				} else {
					timerMinutes = state.timerOption.minutesOnly
					timerSeconds = state.timerOption.secondsOnly
				}
			}
			is TimerStarted -> {
				val timer = state.timer
				if (timerCountMode == CountUp) {
					timerMinutes = timer.minutesSinceStart
					timerSeconds = timer.secondsSinceStart
				} else {
					timerMinutes = timer.minutesLeft
					timerSeconds = timer.secondsLeft
				}
			}
		}
	}
	
	override fun updateTimerColor() {
		val state = presenter.state
		updateTimerBinding()
		when (state) {
			is WaitingToStart -> {
				timerTextColor = EnvVars.color_timerStart
			}
			is TimerStarted -> {
				timerTextColor = state.timer.textColor
			}
		}
	}
	
	override fun updateBells() {
		val state = presenter.state
		if (Prefs.debateBellEnabled && state is HasTimerOption) {
			val timerOption = state.timerOption
			
			if (timerOption.countUpString.isEmpty()) {
				tv_bellsAt.setInvisible()
			} else {
				tv_bellsAt.setVisible()
				
				val bellString = if (timerCountMode == CountUp) {
					timerOption.countUpString
				} else {
					timerOption.countDownString
				}
				
				tv_bellsAt.text = resources.getQuantityString(R.plurals.bells_at, timerOption.bellsSinceStart.count(), bellString)
			}
		} else {
			tv_bellsAt.setInvisible()
		}
	}
	
	override var tv_startPauseText: String
		get() = tv_startPause.text.toString()
		set(value) {
			tv_startPause.text = value
		}
	override val context: Context
		get() = this
	
	override fun crossfadeStartPause() {
		crossFade(tv_startPause, tv_startPause, EnvVars.longAnimTime)
	}
	
	override fun setBegan() {
		fl_timer.setVisible()
		tv_startPause.setVisible()
		tv_tapAnywhereTo.setVisible()
		tv_timerCountMode.setVisible()
		
		tv_startingText.setGone()
		val state = presenter.state
		tv_startPauseText = if (state is TimerStarted && state.running) {
			getString(R.string.pause)
		} else {
			getString(R.string.resume)
		}
		
		updateTimerValue()
		updateTimerColor()
	}
	
	override fun resetBegan() {
		fl_timer.setInvisible()
		tv_startPause.setInvisible()
		tv_tapAnywhereTo.setInvisible()
		tv_timerCountMode.setInvisible()
		
		tv_startingText.setVisible()
		tv_startPauseText = getString(R.string.start)
	}
	
	override fun setTimeButtonAsSelected(view: Button) {
		timerButtons.forEach {
			it.setTextColor(getColorCompat(R.color.buttonUnselected))
		}
		
		view.setTextColor(getColorCompat(R.color.buttonSelected))
	}
	
	
	companion object {
		private const val RetainedFragment = "retained_fragment"
	}
}