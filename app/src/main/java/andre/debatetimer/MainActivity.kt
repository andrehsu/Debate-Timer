package andre.debatetimer

import andre.debatetimer.CountMode.CountDown
import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.extensions.*
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
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.forEach
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private lateinit var model: MainModel

    private lateinit var timerBindings: Map<TimerDisplayMode, TimerBinding>
    private lateinit var timerButtons: List<Button>

    private var timerMinutes: Int = 0
        set(value) {
            field = value
            timerBinding.minutes = value
        }
    private var timerSeconds: Int = 0
        set(value) {
            field = value
            timerBinding.seconds = value
        }
    private var timerTextColor: Int = 0
        set(value) {
            field = value
            timerBinding.color = value
        }
    private var timerBinding: TimerBinding = NullBinding
        set(value) {
            field = value

            updateBellsText()
            updateTextColor()
            updateMinutes()
            updateSeconds()
            for (timerBinding in timerBindings.values) {
                timerBinding.isVisible = timerBinding.timerDisplayMode == this.timerBinding.timerDisplayMode
            }
        }

    private var buttonsActive: Boolean = false
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
    private var keepScreenOn: Boolean = false
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

        model = ViewModelProviders.of(this).get(MainModel::class.java)

        timerBindings = getBindings(this)

        fl_timer.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                fl_timer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                autoSizeTimerText()
            }
        })

        bt_countMode.setOnClickListener { _ ->
            Prefs.countMode.apply(if (Prefs.countMode.value == CountUp) CountDown else CountUp)
        }

        bt_debateBell.setOnClickListener { _ ->
            Prefs.debateBellEnabled.apply(!Prefs.debateBellEnabled.value)
        }

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
                layout.setOnClickListener { v ->
                    model.onTimeButtonSelect((v as Button).text.toString())
                }
                ll_timeButtons.addView(layout)
                timerMaps[layout.text.toString()] = timerOption
                timerButtons += layout
            }
        }
        model.timerMaps = timerMaps

        this.timerButtons = timerButtons

        volumeControlStream = AudioManager.STREAM_MUSIC

        Prefs.debateBellEnabled.observe(this, debateBellObserver)

        Prefs.countMode.observe(this, countModeObserver)

        model.state.observe(this, initUninitObserver)

        model.state.observe(this) { state ->
            when (state) {
                is InitState -> {
                    updateLayoutBinding()
                }

                is WaitingToStart -> {
                    tv_startPause.text = getString(R.string.start)
                    timerTextColor = EnvVars.color_timerStart

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

                    state.timer.textColor.observe(this) {
                        updateTextColor()
                    }

                    state.running.observe(this) { running ->
                        tv_startPause.text = if (running) {
                            getString(R.string.pause)
                        } else {
                            getString(R.string.resume)
                        }
                        buttonsActive = running
                        keepScreenOn = running
                    }
                }
            }
        }

        model.selectedButton.observe(this) { txt ->
            timerButtons.forEach {
                if (it.text == txt) {
                    it.setTextColor(getColorCompat(R.color.buttonSelected))
                } else {
                    it.setTextColor(getColorCompat(R.color.buttonUnselected))
                }
            }
        }
    }

    private fun updateTextColor() {
        val state = model.state.value
        if (state is TimerStarted) {
            timerTextColor = state.timer.textColor.value
        } else {
            timerTextColor = EnvVars.color_timerStart
        }
    }

    private fun autoSizeTimerText() {
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

    private fun updateLayoutBinding() {
        val state = model.state.value
        timerBinding = timerBindings.getValue(
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

    private fun updateBellsText() {
        val state = model.state.value
        if (Prefs.debateBellEnabled.value) {
            bt_debateBell.text = when (state) {
                is InitState -> ""
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

    private fun updateMinutes() {
        val state = model.state.value
        timerMinutes = when (state) {
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

    private fun updateSeconds() {
        val state = model.state.value
        timerSeconds = when (state) {
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

    private val debateBellObserver = Observer<Boolean> { debateBellEnabled ->
        updateBellsText()

        if (debateBellEnabled!!) {
            bt_debateBell.icon = getDrawable(R.drawable.ic_notifications_active_white_24dp)
        } else {
            bt_debateBell.icon = getDrawable(R.drawable.ic_notifications_off_white_24dp)
        }
    }

    private val countModeObserver = Observer<CountMode> { countMode ->
        updateLayoutBinding()

        bt_countMode.text = getString(
                if (countMode == CountDown) {
                    R.string.count_down
                } else {
                    R.string.count_up
                }
        )
    }

    private val initUninitObserver = Observer<State> { state ->
        when (state) {
            is InitState -> {
                fl_timer.setInvisible()
                tv_startPause.setInvisible()
                tv_startingText.setVisible()
            }
            else -> {
                fl_timer.setVisible()
                tv_startPause.setVisible()
                tv_startingText.setGone()
            }
        }
    }

    fun onStartPause(@Suppress("UNUSED_PARAMETER") v: View) {
        model.onStartPause()
    }
}