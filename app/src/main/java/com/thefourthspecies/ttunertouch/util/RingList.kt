package com.thefourthspecies.ttunertouch.util


/**
 * Created by Graham on 2018-01-31.
 */
class RingList<T : Comparable<T>>(items: Iterable<T>) : Iterable<T> {
    constructor() : this(emptyList<T>())

    enum class Direction {
        ASCENDING,
        DESCENDING
    }

    private var mFirst: T? = null
    private val mItems: MutableMap<T, Pointers<T>> = listToItems(items)

    private fun listToItems(items: Iterable<T>): MutableMap<T, Pointers<T>> {
        val map = mutableMapOf<T, Pointers<T>>()
        val sortedItems = items.toSortedSet() // to remove duplicates
        mFirst = sortedItems.firstOrNull()

        for (i in sortedItems) {
            addTo(map, i, mFirst)
        }
        return map
    }

    fun remove(item: T) {
        removeFrom(mItems, item, mFirst)
    }

    private fun removeFrom(map: MutableMap<T, Pointers<T>>, item: T, first: T?) {
        assert(map.contains(item)) {
            "Cannot remove item that is not in RingList: $item"
        }

        var prev = map[item]!!.prev
        var next = map[item]!!.next
        map[prev]!!.next = next
        map[next]!!.prev = prev
        map.remove(item)

        if (item == first) {
            mFirst = if (map.containsKey(next)) {
                next
            } else {
                assert(map.isEmpty()) {
                    "RingList: There is no mFirst but mItems is not empty"
                }
                null
            }
        }
    }

    fun add(item: T) {
        if (mItems.containsKey(item)) {
            removeFrom(mItems, item, mFirst)
        }
        addTo(mItems, item, mFirst)
    }

    private fun addTo(map: MutableMap<T, Pointers<T>>, item: T, first: T?) {
        assert(!map.containsKey(item)) {
            "Trying to add an item that is already in the RingList: item=$item"
        }

        if (map.isEmpty() || first == null) {
            mFirst = item
            map[item] = Pointers(item, item)
            return
        }

        if (item <= first) {
            mFirst = item
        }

        val prev = findPrevious(map, item, first)
        val next = map[prev]!!.next
        map[item] = Pointers(prev, next)
        map[prev]!!.next = item
        map[next]!!.prev = item
    }

    private fun findPrevious(map: MutableMap<T, Pointers<T>>, item: T, first: T): T {
        val next = iterateUntilExcluding(map, first, first,
            Direction.ASCENDING
        ).find { it >= item }
        val prev = if (next == null) {
            iterateUntilExcluding(map, first, first,
                Direction.ASCENDING
            ).last()
        } else {
            map[next]!!.prev
        }
        return prev
    }


    override fun toString(): String {
        return mItems.keys.toList().joinToString(
                ", ",
                "RingList(",
                ")"
        )
    }

    // Iterates from start until reaching end. If start and end are the same, will visit every element
    private fun iterateUntilExcluding(map: MutableMap<T, Pointers<T>>, start: T?, end: T?, direction: Direction): Iterable<T> {
        if (map.isEmpty() || start == null || end == null) {
            return emptyList<T>()
        }

        return object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return RingIterator(
                    map,
                    start,
                    end,
                    direction
                )
            }
        }
    }

    // Iterates from start, visiting all elements, in given direction
    fun iterateFrom(start: T, direction: Direction): Iterable<T> {
        return iterateUntilExcluding(mItems, start, start, direction)
    }

    // Iterates from start to element prior to end, visiting all elements in given direction.
    // If start and end are the same, then visits no elements.
    fun iterateUntilExcluding(start: T, end: T, direction: Direction): Iterable<T> {
        return if (start == end) {
            emptyList<T>()
        } else {
            iterateUntilExcluding(mItems, start, end, direction)
        }

    }

    override fun iterator(): Iterator<T> {
        return iterateUntilExcluding(mItems, mFirst, mFirst,
            Direction.ASCENDING
        ).iterator()
    }

    private class RingIterator<T>(val map: MutableMap<T, Pointers<T>>, val start: T, val end: T, val direction: Direction) : AbstractIterator<T>() {
        var curr = start
        var isStarting = true

        init {
            assert(map.contains(start) && map.containsKey(end)) {
                "RingIterator starting or ending at item that is not present in RingList: start=$start, end=$end"
            }
        }

        override fun computeNext() {
            if (curr != end || isStarting) {
                setNext(curr)

                val pointers = map[curr]!!
                curr = if (direction == Direction.ASCENDING) pointers.next else pointers.prev
                isStarting = false
            } else {
               done()
            }
        }
    }

    private data class Pointers<T>(var prev: T, var next: T)



}
//
///**
// * Created by Graham on 2018-01-29.
// */
//class NoteRing<Note>(notes: List<Note>) {
//    val mNotes: MutableMap<Note, RingEntry> = listToEntries(notes)
//
//    private fun listToEntries(unsortedItems: List<Note>): MutableMap<Note, RingEntry> {
//        val items = unsortedItems.toMutableList().sortBy {  }
//
//    }
//
//    data class RingEntry(var prev: Note, var next: Note)
//
//}