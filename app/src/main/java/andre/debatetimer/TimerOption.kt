package andre.debatetimer

import andre.debatetimer.DebateBell.ONCE
import andre.debatetimer.extensions.secondsToString
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug

/**
 * Created by Andre on 5/5/2017.
 */
class TimerOption(val seconds: Int, val bellsSinceStart: Map<Int, DebateBell>) {
	companion object : AnkoLogger {
		private val DEFAULT = TimerOption(420, mapOf(60 to ONCE, 360 to ONCE))
		private val cache = mutableMapOf<String, TimerOption>()
		
		fun parseTag(tag: String): TimerOption {
			@Suppress("NAME_SHADOWING")
			val tag = tag.filterNot { it == ' ' }
			return cache.getOrPut(tag) {
				val tokens = tag.split(';')
				
				try {
					val seconds = tokens[0].toInt()
					
					val bells = if (tokens[1].isEmpty()) {
						mapOf()
					} else if (tokens[1].toIntOrNull() == -1) {
						mapOf(60 to ONCE, seconds - 60 to ONCE)
					} else {
						val bellTokens = tokens[1].split(',')
						bellTokens.map { it.toInt() to ONCE }.toMap()
					}
					
					TimerOption(seconds, bells)
				} catch(e: RuntimeException) {
					debug { "Error occurred while parsing \"$tag\" for TimerOption" }
					DEFAULT
				}
			}
		}
	}
	
	val countUpString = bellsSinceStart.filter { (_, v) -> v == ONCE }.map { secondsToString(it.key) }.joinToString()
	val countDownString = bellsSinceStart.filter { (_, v) -> v == ONCE }.map { secondsToString(seconds - it.key) }.joinToString()
	
	val minutesOnly = seconds / 60
	val secondsOnly = seconds % 60
}