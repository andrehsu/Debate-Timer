package andre.debatetimer.timer

import andre.debatetimer.secondsToString
import andre.debatetimer.timer.DebateBell.Once

class TimerOption(val seconds: Int, bellsSinceStart: Map<Int, DebateBell>) {
	companion object {
		private val cache = mutableMapOf<String, TimerOption>()
		
		fun parseTag(tag: String): TimerOption? {
			@Suppress("NAME_SHADOWING")
			val tag = tag.filterNot { it == ' ' }
			val tokens = tag.split(';')
			
			return try {
				val seconds = tokens[0].toInt()
				
				val bells = when {
					tokens[1].isEmpty() -> mapOf()
					tokens[1].toIntOrNull() == -1 -> mapOf(60 to Once, seconds - 60 to Once)
					else -> {
						val bellTokens = tokens[1].split(',')
						bellTokens.map { it.toInt() to Once }.toMap()
					}
				}
				
				val ret = TimerOption(seconds, bells)
				
				cache[tag] = ret
				
				ret
			} catch (e: RuntimeException) {
				null
			}
		}
	}
	
	val countUpString: String
	val countDownString: String
	val bellsSinceStart: Map<Int, DebateBell>
	
	init {
		this.bellsSinceStart = bellsSinceStart.toSortedMap()
		val sorted = this.bellsSinceStart.filter { (_, v) -> v == Once }.map { it.key }
		
		countUpString = sorted.joinToString { secondsToString(it) }
		countDownString = sorted.joinToString { secondsToString(seconds - it) }
	}
	
	val minutesOnly = seconds / 60
	val secondsOnly = seconds % 60
}