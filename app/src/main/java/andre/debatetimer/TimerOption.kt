package andre.debatetimer

import andre.debatetimer.extensions.DebateBell
import andre.debatetimer.extensions.DebugLogger
import andre.debatetimer.extensions.invoke
import andre.debatetimer.extensions.secondsToString

/**
 * Created by Andre on 5/5/2017.
 */
enum class TimerOption(val seconds: Int, vararg bellsSinceStart: Pair<Int, DebateBell>) {
	THREE_SECONDS(3, 1 to DebateBell.ONCE),
	TWO_MINUTES(120),
	THREE_MINUTES(180),
	FOUR_MINUTES(240),
	FIVE_MINUTES(300, 60 to DebateBell.ONCE, 240 to DebateBell.ONCE),
	SEVEN_MINUTES(420, 60 to DebateBell.ONCE, 360 to DebateBell.ONCE),
	EIGHT_MINUTES(480, 60 to DebateBell.ONCE, 420 to DebateBell.ONCE);
	
	val bellsSinceStart = bellsSinceStart.toMap()
	
	companion object : DebugLogger {
		fun parseKey(tag: String): TimerOption {
			return when (tag) {
				"3" -> THREE_SECONDS
				"120" -> TWO_MINUTES
				"180" -> THREE_MINUTES
				"240" -> FOUR_MINUTES
				"300" -> FIVE_MINUTES
				"420" -> SEVEN_MINUTES
				"480" -> EIGHT_MINUTES
				else -> {
					debug { "Invalid key ($tag) passed to parseKey" }
					SEVEN_MINUTES
				}
			}
		}
	}
	
	val countUpPoiString = bellsSinceStart.filter { it.second == DebateBell.ONCE }.map { secondsToString(it.first) }.toString()(1, -1)
	val countDownPoiString = bellsSinceStart.filter { it.second == DebateBell.ONCE }.map { secondsToString(seconds - it.first) }.toString()(1, -1)
	
	val secondsOnly = seconds % 60
	val minutesOnly = seconds / 60
}