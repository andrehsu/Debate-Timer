package org.debatetimer.timer

import org.debatetimer.timer.DebateBell.Once
import kotlin.math.absoluteValue

class TimerConfiguration(val tag: String, val totalSeconds: Int, bellsSinceStart: Map<Int, DebateBell>) {
    companion object {
        val default = TimerConfiguration("default", 180, mapOf())
        
        private val cache = mutableMapOf<String, TimerConfiguration>()
        
        fun parseTag(tag: String): TimerConfiguration {
            @Suppress("NAME_SHADOWING")
            val tag = tag.filterNot { it == ' ' }
            val tokens = tag.split(';')
            
            val seconds = tokens[0].toInt()
            
            val bells = when {
                tokens[1].isEmpty() -> mapOf()
                tokens[1].toIntOrNull() == -1 -> mapOf(60 to Once, seconds - 60 to Once)
                else -> {
                    val bellTokens = tokens[1].split(',')
                    bellTokens.map { it.toInt() to Once }.toMap()
                }
            }
            
            val ret = TimerConfiguration(tag, seconds, bells)
            
            cache[tag] = ret
            
            return ret
        }
    }
    
    val countUpBellsText: String
    val countDownBellsText: String
    val bellsSinceStart: Map<Int, DebateBell>
    
    init {
        this.bellsSinceStart = bellsSinceStart.toSortedMap()
        val sorted = this.bellsSinceStart.filter { (_, v) -> v == Once }.map { it.key }
        
        countUpBellsText = sorted.joinToString { secondsToString(it) }
        countDownBellsText = sorted.joinToString { secondsToString(totalSeconds - it) }
    }
    
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    val text: String
    
    init {
        val buttonTextSb = StringBuilder()
        if (minutes != 0) {
            buttonTextSb.append("${minutes}m")
        }
        if (seconds != 0) {
            buttonTextSb.append("${seconds}s")
        }
        
        text = buttonTextSb.toString()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TimerConfiguration
        
        if (totalSeconds != other.totalSeconds) return false
        if (countUpBellsText != other.countUpBellsText) return false
        if (countDownBellsText != other.countDownBellsText) return false
        if (bellsSinceStart != other.bellsSinceStart) return false
        if (minutes != other.minutes) return false
        if (seconds != other.seconds) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = totalSeconds
        result = 31 * result + countUpBellsText.hashCode()
        result = 31 * result + countDownBellsText.hashCode()
        result = 31 * result + bellsSinceStart.hashCode()
        result = 31 * result + minutes
        result = 31 * result + seconds
        return result
    }
    
    override fun toString(): String {
        return "TimerOption(totalSeconds=$totalSeconds, countUpBellsText='$countUpBellsText', countDownBellsText='$countDownBellsText', bellsSinceStart=$bellsSinceStart, minutes=$minutes, seconds=$seconds)"
    }
}

private fun secondsToString(seconds: Int): String {
    val abs = seconds.absoluteValue
    val minutes = abs / 60
    val secondsOnly = abs % 60
    return if (seconds >= 0) {
        ""
    } else {
        "-"
    } + "$minutes:${secondsOnly.toString().padStart(2, '0')}"
}