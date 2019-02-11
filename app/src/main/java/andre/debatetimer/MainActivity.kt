package andre.debatetimer

import andre.debatetimer.CountMode.CountDown
import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.extensions.defaultSharedPreferences
import andre.debatetimer.extensions.setGone
import andre.debatetimer.extensions.setInvisible
import andre.debatetimer.extensions.setVisible
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.marginBottom
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var model: MainModel
    
    private lateinit var timerBindings: Map<TimerDisplayMode, TimerBinding>
    private lateinit var timerButtons: List<Button>
    
    /**
     * Property holding the current binding for views
     */
    private var timerView: TimerBinding = NullBinding
        set(value) {
            field = value
            updateBellsText()
            updateTextColor()
            updateMinutes()
            updateSeconds()
            for (timerBinding in timerBindings.values) {
                timerBinding.isVisible = timerBinding.timerDisplayMode == this.timerView.timerDisplayMode
            }
        }
    /**
     * Property that controls whether the timer option buttons are selectable
     */
    private var timerOptionButtonsSelectable: Boolean = false
        set(value) {
            field = value
            if (value) {
                timerButtons.forEach {
                    it.isClickable = false
                    it.alpha = 0.38f
                }
            } else {
                timerButtons.forEach {
                    it.isClickable = true
                    it.alpha = 1.0f
                }
            }
        }
    /**
     * Property that controls whether the screen is kept on
     */
    private var keepScreenOn: Boolean = false
        set(value) {
            field = value
            if (value) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    
    /**
     * onCreate method
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        model = ViewModelProviders.of(this).get(MainModel::class.java)
        
        timerBindings = getBindings(this)
        
        fl_timer.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                fl_timer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                fitTimerTextSize()
            }
        })
        
        val sp = defaultSharedPreferences
        
        val timersStr = sp.getStringSet(getString(R.string.pref_timers), mutableSetOf(
                "180;-1",
                "240;-1",
                "300;-1",
                "360;-1",
                "420;-1",
                "480;-1"))!!.sorted()
        
        val timerMaps = mutableMapOf<String, TimerOption>()
        val timerButtons = mutableListOf<Button>()
        
        timersStr.forEach { str ->
            val timerOption = TimerOption.parseTag(str)
            
            if (timerOption != null) {
                val layout = layoutInflater.inflate(R.layout.timer_button, ll_timeButtons, false) as Button
                
                layout.text = with(timerOption) {
                    if (minutes != 0 && seconds != 0) {
                        "${minutes}m${seconds}s"
                    } else if (minutes != 0) {
                        "${minutes}m"
                    } else if (seconds != 0) {
                        "${seconds}s"
                    } else {
                        "nil"
                    }
                }
                layout.setTextColor(getColorCompat(R.color.buttonUnselected))
                ll_timeButtons.addView(layout)
                timerMaps[layout.text.toString()] = timerOption
                timerButtons += layout
            }
        }
        model.timerMaps = timerMaps
        
        this.timerButtons = timerButtons
        
        volumeControlStream = AudioManager.STREAM_MUSIC
        
        Prefs.debateBellEnabled.observe(this, Observer { debateBellEnabled ->
            updateBellsText()
            
            if (debateBellEnabled!!) {
                bt_debateBell.icon = getDrawable(R.drawable.ic_notifications_active_white_24dp)
            } else {
                bt_debateBell.icon = getDrawable(R.drawable.ic_notifications_off_white_24dp)
            }
        })
        
        Prefs.countMode.observe(this, Observer { countMode ->
            updateLayoutBinding()
            
            bt_countMode.text = getString(
                    if (countMode == CountDown) {
                        R.string.count_down
                    } else {
                        R.string.count_up
                    }
            )
        })
        
        model.state.observe(this, Observer { state ->
            when (state) {
                is InitState -> {
                    fl_timer.setInvisible()
                    bt_startPause.setGone()
                    bt_resetTime.setGone()
                    
                    tv_startingText.setVisible()
                }
                else -> {
                    TransitionManager.beginDelayedTransition(root_activity_main)
                    fl_timer.setVisible()
                    bt_startPause.setVisible()
                    
                    tv_startingText.setGone()
                }
            }
        })
        
        model.state.observe(this, Observer { state ->
            val rootView = root_activity_main
            
            TransitionManager.beginDelayedTransition(rootView)
            if (state is TimerStarted) {
                bt_resetTime.setVisible()
            } else {
                bt_resetTime.setGone()
            }
        })
        
        model.state.observe(this) { state ->
            when (state) {
                is InitState -> {
                    updateLayoutBinding()
                }
                
                is WaitingToStart -> {
                    bt_startPause.text = getString(R.string.start)
                    timerView.color = EnvVars.color_timerStart
                    
                    updateLayoutBinding()
                }
                is TimerStarted -> {
                    updateBellsText()
                    
                    state.timer.negative.observe(this) {
                        updateLayoutBinding()
                    }
                    
                    state.timer.ended.observe(this) {
                        updateLayoutBinding()
                    }
                    
                    state.timer.minutesCountUp.observe(this) {
                        updateMinutes()
                    }
                    
                    state.timer.secondsCountUp.observe(this) {
                        updateSeconds()
                    }
                    
                    state.timer.textColor.observe(this) { textColor ->
                        timerView.color = textColor
                    }
                    
                    state.running.observe(this) { running ->
                        TransitionManager.beginDelayedTransition(root_activity_main)
                        bt_startPause.text = if (running) {
                            getString(R.string.pause)
                        } else {
                            getString(R.string.resume)
                        }
                        timerOptionButtonsSelectable = running
                        keepScreenOn = running
                    }
                }
            }
        }
        
        model.selectedButtonStr.observe(this) { txt ->
            timerButtons.forEach {
                if (it.text == txt) {
                    it.setTextColor(getColorCompat(R.color.buttonSelected))
                } else {
                    it.setTextColor(getColorCompat(R.color.buttonUnselected))
                }
            }
        }
    }
    
    /**
     * Updates the timer text color
     */
    private fun updateTextColor() {
        val state = model.state.value
        timerView.color = if (state is TimerStarted) {
            state.timer.textColor.value
        } else {
            EnvVars.color_timerStart
        }
    }
    
    /**
     * Fits the timer text size to the given device
     */
    private fun fitTimerTextSize() {
        val frameWidth = fl_timer.width.toFloat()
        val width = timer_normal.width
        val widthScale = (frameWidth / width)
        
        val frameHeight = fl_timer.height.toFloat()
        val height = timer_normal.height
        val heightScale = (frameHeight / height)
        
        val scale = minOf(widthScale, heightScale)
        
        fl_timer.forEach { viewGroup ->
            viewGroup.scaleX = scale
            viewGroup.scaleY = scale
            (viewGroup as ViewGroup).forEach {
                if (it is TextView && it.text == ":") {
                    (it.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = (it.marginBottom * scale).toInt()
                }
            }
        }
    }
    
    /**
     * Updates the layout binding
     */
    private fun updateLayoutBinding() {
        val state = model.state.value
        timerView = timerBindings.getValue(
                when (state) {
                    is InitState -> TimerDisplayMode.Null
                    is WaitingToStart -> TimerDisplayMode.Normal
                    is TimerStarted -> {
                        val timer = state.timer
                        if (timer.ended.value) {
                            TimerDisplayMode.End
                        } else if (timer.negative.value && Prefs.countMode.value == CountDown) {
                            TimerDisplayMode.Negative
                        } else {
                            TimerDisplayMode.Normal
                        }
                    }
                }
        )
    }
    
    /**
     * Updates the bell text shown on the toggle bell button
     */
    private fun updateBellsText() {
        val state = model.state.value
        if (Prefs.debateBellEnabled.value) {
            bt_debateBell.text = when (state) {
                is InitState -> "On"
                is HasTimerOption -> {
                    val bellString = if (Prefs.countMode.value == CountUp) {
                        state.timerOption.countUpString
                    } else {
                        state.timerOption.countDownString
                    }
                    resources.getString(R.string.bells_at, bellString)
                }
                else -> "--Error--"
            }
        } else {
            bt_debateBell.text = getString(R.string.off)
        }
    }
    
    /**
     * Updates the minutes of the timer
     */
    private fun updateMinutes() {
        val state = model.state.value
        timerView.minutes = when (state) {
            is InitState -> 0
            is WaitingToStart ->
                if (Prefs.countMode.value == CountUp) {
                    0
                } else {
                    state.timerOption.minutes
                }
            is TimerStarted ->
                if (Prefs.countMode.value == CountUp) {
                    state.timer.minutesCountUp.value
                } else {
                    state.timer.minutesCountDown.value
                }
        }
    }
    
    /**
     * Updates the seconds of the timer
     */
    private fun updateSeconds() {
        val state = model.state.value
        timerView.seconds = when (state) {
            is InitState -> 0
            is WaitingToStart ->
                if (Prefs.countMode.value == CountUp) {
                    0
                } else {
                    state.timerOption.seconds
                }
            is TimerStarted ->
                if (Prefs.countMode.value == CountUp) {
                    state.timer.secondsCountUp.value
                } else {
                    state.timer.secondsCountDown.value
                }
        }
    }
    
    override fun onBackPressed() {
        class ExitDialogFragment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.exit_question)
                        .setPositiveButton(android.R.string.yes) { _, _ -> this@MainActivity.finish() }
                        .setNegativeButton(android.R.string.no) { _, _ -> dialog.cancel() }
                        .create()
            }
        }
        
        if (isTaskRoot) {
            ExitDialogFragment().show(supportFragmentManager, null)
        } else {
            super.onBackPressed()
        }
    }
    
    
    fun onDebateBellClick(v: View) {
        require(v.id == R.id.bt_debateBell)
        
        Prefs.debateBellEnabled.apply(!Prefs.debateBellEnabled.value)
    }
    
    fun onCountModeClick(v: View) {
        require(v.id == R.id.bt_countMode)
        
        Prefs.countMode.apply(if (Prefs.countMode.value == CountUp) CountDown else CountUp)
    }
    
    fun onTimerButtonClick(v: View) {
        require(v.id == R.id.bt_timerButton)
        
        v as Button
        model.onTimeButtonSelect(v.text.toString())
    }
    
    fun onStartPause(v: View) {
        require(v.id == R.id.bt_startPause)
        
        model.onStartPause()
    }
    
    fun onResetTimeClick(v: View) {
        require(v.id == R.id.bt_resetTime)
        
        model.onResetTime()
    }
}