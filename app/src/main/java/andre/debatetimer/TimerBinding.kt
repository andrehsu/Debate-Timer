package andre.debatetimer

import andre.debatetimer.extensions.setGone
import andre.debatetimer.extensions.setVisible
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

interface TimerBinding {
	val timerDisplayMode: TimerDisplayMode
	
	var minutes: Int
	
	var seconds: Int
	
	var isVisible: Boolean
	
	var color: Int
}

enum class TimerDisplayMode {
	Normal, Negative, End, Null
}

object NullBinding : TimerBinding {
	override val timerDisplayMode: TimerDisplayMode = TimerDisplayMode.Normal
	override var minutes: Int = 0
	override var seconds: Int = 0
	override var isVisible: Boolean = false
	override var color: Int = 0
}

fun getBindings(mainActivity: MainActivity): Map<TimerDisplayMode, TimerBinding> {
	with(mainActivity) {
		val normal = object : TimerBinding {
			private val minutesView = timer_normal.findViewById<TextView>(R.id.tv_timer_m)
			private val secondsView = timer_normal.findViewById<TextView>(R.id.tv_timer_s)
			private val colon = timer_normal.findViewById<TextView>(R.id.tv_timer_colon)
			
			override val timerDisplayMode: TimerDisplayMode = TimerDisplayMode.Normal
			override var minutes: Int = 0
				set(value) {
					field = value
					minutesView.text = field.toString()
				}
			override var seconds: Int = 0
				set(value) {
					field = value
					secondsView.text = field.toString().padStart(2, '0')
				}
			override var isVisible: Boolean = false
				set(value) {
					field = value
					if (field) {
						timer_normal.setVisible()
					} else {
						timer_normal.setGone()
					}
				}
			override var color: Int = 0
				set(value) {
					field = value
					minutesView.setTextColor(field)
					secondsView.setTextColor(field)
					colon.setTextColor(field)
				}
		}
		
		val negative = object : TimerBinding {
			private val secondsView = timer_negative.findViewById<TextView>(R.id.tv_timer_s)
			private val negSign = timer_negative.findViewById<TextView>(R.id.tv_timer_negSign)
			
			override val timerDisplayMode: TimerDisplayMode = TimerDisplayMode.Negative
			override var minutes: Int = 0
			override var seconds: Int = 0
				set(value) {
					field = value
					secondsView.text = (minutes * 60 + seconds).toString().padStart(2, '0')
				}
			override var isVisible: Boolean = false
				set(value) {
					field = value
					if (field) {
						timer_negative.setVisible()
					} else {
						timer_negative.setGone()
					}
				}
			override var color: Int = 0
				set(value) {
					field = value
					secondsView.setTextColor(field)
					negSign.setTextColor(field)
				}
		}
		
		val end = object : TimerBinding {
			override val timerDisplayMode: TimerDisplayMode = TimerDisplayMode.End
			override var minutes: Int = 0
			override var seconds: Int = 0
			override var isVisible: Boolean = false
				set(value) {
					field = value
					if (field) {
						timer_end.setVisible()
					} else {
						timer_end.setGone()
					}
				}
			override var color: Int = 0
		}
		
		return mapOf(TimerDisplayMode.Normal to normal,
				TimerDisplayMode.End to end,
				TimerDisplayMode.Negative to negative).withDefault { NullBinding }
	}
}