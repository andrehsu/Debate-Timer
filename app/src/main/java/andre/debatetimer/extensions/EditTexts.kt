@file:Suppress("NOTHING_TO_INLINE")

package andre.debatetimer.extensions

import android.widget.EditText

inline var EditText.textStr: String
    get() = this.text.toString()
    set(value) = this.setText(value)

inline fun EditText.clearText() = this.setText("")

inline fun EditText.isEmpty(): Boolean = this.textStr.isEmpty()
