package andre.debatetimer.extensions

operator fun CharSequence.invoke(): String = toString()

operator fun CharSequence.invoke(start: Int = 0, end: Int = length): String = substring(start, if (end >= 0) end else end + length)

operator fun String.invoke(start: Int = 0, end: Int = length, step: Int): String = slice(start..(if (end >= 0) end else end + length) step step)