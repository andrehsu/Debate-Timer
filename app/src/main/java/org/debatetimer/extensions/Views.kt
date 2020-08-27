@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.debatetimer.extensions

import android.view.View

inline fun View.setVisible() {
    this.visibility = View.VISIBLE
}

inline fun View.setInvisible() {
    this.visibility = View.INVISIBLE
}

inline fun View.setGone() {
    this.visibility = View.GONE
}
