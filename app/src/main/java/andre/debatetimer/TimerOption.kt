package andre.debatetimer

import andre.debatetimer.extensions.DebateBell
import andre.debatetimer.extensions.DebateBell.ONCE
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
			return cache.getOrPut(tag) {
				val noSpace = tag.filterNot { it == ' ' }
				val tokens = noSpace.split(';')
				
				try {
					val seconds = tokens[0].toInt()
					
					val bells = if (tokens[1].isEmpty()) {
						mapOf()
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