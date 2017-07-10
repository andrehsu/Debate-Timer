@file:Suppress("NOTHING_TO_INLINE")

package andre.debatetimer.extensions

import java.util.*

inline fun <E> Set<E>.unmodifiable(): Set<E> = Collections.unmodifiableSet(this)

inline fun <E> List<E>.unmodifiable(): List<E> = Collections.unmodifiableList(this)

inline fun <E> Collection<E>.unmodifiable(): Collection<E> = Collections.unmodifiableCollection(this)

inline fun <K, V> Map<K, V>.unmodifiable(): Map<K, V> = Collections.unmodifiableMap(this)

inline fun <E> SortedSet<E>.unmodifiable(): SortedSet<E> = Collections.unmodifiableSortedSet(this)

inline fun <K, V> SortedMap<K, V>.unmodifiable(): SortedMap<K, V> = Collections.unmodifiableSortedMap(this)