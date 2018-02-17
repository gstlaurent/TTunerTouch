package com.thefourthspecies.ttunertouch

import android.util.Log
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.abs

const val LOW_FREQ = 400.0
const val HIGH_FREQ = 800.0
const val HERTZ_TOLERANCE = 10e-4 // If it is too precise, then there will be conflicts

typealias Hertz = Double

enum class Comma(val ratio: Double, val symbol: String) {
    PYTHAGOREAN(531441.0/524288.0, "P"), // (12*P5)/(7*P8) = ((3/2)^12)/(2^7) = (3^12)/(2^19)
    SYNTONIC(81.0/80.0, "S"), // (4*P5)/(1*M3+2*P8) = (3/2)^4/(5/4 * 2^2) = (3^4)/((2^4)*5)
    ENHARMONIC(128.0/125.0, "E"), // P8/3*M3 = (2^12)/((5/4)^3) = (2^7)/(5^3)
    PURE(1.0, "=")
}

// fraction: a positive or negative (or 0) value indicating the direction and amount of temper by the comma
data class Temper(val interval: Interval, val fraction: Double, val comma: Comma) {
    val temperedRatio: Double
        get() = interval.ratio * commaFractionRatio

    private val commaFractionRatio: Double
        get() = Math.pow(comma.ratio, fraction)
}

class Relationship(val fromNote: Note, val toNote: Note, val temper: Temper) {

    fun isBetween(note1: Note, note2: Note): Boolean {
        return  (note1 == fromNote && note2 == toNote) ||
                (note2 == fromNote && note1 == toNote)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Relationship) return false

        if (fromNote != other.fromNote) return false
        if (toNote != other.toNote) return false
        if (temper != other.temper) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fromNote.hashCode()
        result = 31 * result + toNote.hashCode()
        result = 31 * result + temper.hashCode()
        return result
    }

    override fun toString(): String {
        return "Relationship(temper=$temper, note1=$fromNote, note2=$toNote)"
    }
}


class Temperament(referenceNote: Note, referencePitch: Hertz) {
    private val relationshipGraph = HashMap<Note, MutableList<Destination>>()
    private val pitches = HashMap<Note, Hertz?>()

    var referenceNote: Note = referenceNote
        set(note) {
            field = note
            invalidate()
        }

    var referencePitch: Hertz = referencePitch
        set(freq) {
            field = freq
            invalidate()
        }

    init {
        invalidate()
    }

    val relationships: List<Relationship>
        get() {
            val relSet = mutableSetOf<Relationship>()
            val rels: List<Relationship> = relationshipGraph.flatMap {
                (note, dests) -> dests.map { Relationship(note, it.note, it.temper)  }
            }
            return rels
        }

    val notes: List<Note>
        get() = relationshipGraph.keys.toList()

    fun pitchOf(note: Note): Hertz? = note.pitch

    fun addNote(note: Note) {
        if (!relationshipGraph.containsKey(note)) {
            relationshipGraph[note] = mutableListOf<Destination>()
        }
    }

    fun removeNote(note: Note) {
        if (!relationshipGraph.contains(note)) return

        val dests = destinationsFrom(note)
        relationshipGraph.remove(note)

        for ((dNote, _) in dests) {
            removeReferences(dNote, note)
        }

        pitches.remove(note)
    }

    fun setRelationship(from: Note, to: Note, temper: Temper) {
        replaceRelationship(from, to, temper)

        updatePitch(from, to, temper)

        invalidate()

    }

    private fun replaceRelationship(from: Note, to: Note, temper: Temper) {
        removeReferences(from, to)
        removeReferences(to, from)

        destinationsFrom(from).add(Destination(to, temper))
        destinationsFrom(to).add(Destination(from, temper))
    }

    fun removeRelationship(note1: Note, note2: Note) {
        removeReferences(note1, note2)
        removeReferences(note2, note1)

        invalidate()
    }

    private fun removeReferences(fromNote: Note, toNote: Note) {
        if (relationshipGraph.containsKey(fromNote)) {
            destinationsFrom(fromNote).removeAll {
                it.note == toNote
            }
        }
    }

    private fun destinationsFrom(note: Note): MutableList<Destination> {
        return relationshipGraph.getOrPut(note) { mutableListOf<Destination>() }
    }

    private fun updatePitch(from: Note, to: Note, temper: Temper) {
        if (to == referenceNote) return
        when {
            from.hasPitch() && to.hasPitch() -> updatePitchFromBase(from, to, temper)
            from.hasPitch() -> updatePitchFromBase(from, to, temper)
            to.hasPitch() -> updatePitchFromBase(to, from, temper)
            else -> {/*neither has a pitch, so nothing to update*/}
        }
    }

    private fun updatePitchFromBase(base: Note, dest: Note, temper: Temper) {
        val basePitch = base.pitch
        if (basePitch != null) {
            val direction = chromaticIntervalDirection(base, dest, temper.interval) // Allows chromatically-equivalent intervals
//            val direction = intervalDirection(base, dest, temper.interval) // Does not allow chromatically-equivalent intervals (e.g., C# to Ab for a P5)
            val pitch = calculateTemperedPitch(basePitch, direction, temper)
            deleteConflictingRelationships(dest, pitch, base)
            dest.pitch = pitch
        } else {
            Log.d(DEBUG_TAG,
                    "${::updatePitchFromBase.name}: " +
                            "Temperament pitches inconsistent because base pitch is null." +
                            " base=$base, dest=$dest, temper=$temper")
            // this function is called from .invalidate(), so cannot call that here
        }
    }

    private fun intervalDirection(from: Note, to: Note, interval: Interval): Direction {
        return when {
            from.atIntervalAbove(interval) == to -> Direction.ASCENDING
            to.atIntervalAbove(interval) == from -> Direction.DESCENDING
            else -> throw Exception("Interval does not apply for notes: from=$from, to=$to, interval=$interval")
        }
    }

    private fun chromaticIntervalDirection(from: Note, to: Note, interval: Interval): Direction {
        return when (interval.chromaticDifference) {
            to chromaticMinus from -> Direction.ASCENDING
            from chromaticMinus to -> Direction.DESCENDING
            else -> throw Exception("Chromatic interval does not apply for notes: from=$from, to=$to, interval=$interval")
        }
    }

    // Delete relationships not from goodNeighbour, if pitches don't match
    private fun deleteConflictingRelationships(note: Note, goodPitch: Hertz, goodNeighbour: Note) {
        val prevPitch = note.pitch
        if (prevPitch != null && !prevPitch.equalsWithinTolerance(goodPitch)) {
            val conflicts = destinationsFrom(note).filter { it.note != goodNeighbour }
            for ((conflict, _) in conflicts) {
                removeRelationship(note, conflict)
            }
        }
    }

    private fun calculateTemperedPitch(basePitch: Hertz, direction: Direction, temper: Temper): Hertz {
        val temperedPitch = if (direction == Direction.ASCENDING) {
            basePitch * temper.temperedRatio
        } else {
            basePitch / temper.temperedRatio
        }
        return normalize(temperedPitch)
    }

    private fun normalize(pitch: Hertz): Hertz {
        assert(pitch > 0) {
            "Can only normalize pitch greater than 0. Given pitch=$pitch"
        }
        return when {
            pitch >= HIGH_FREQ -> normalize(pitch / 2.0)
            pitch < LOW_FREQ -> normalize(pitch * 2.0)
            else -> pitch
        }
    }

    private fun invalidate() {
        pitches.clear()
        calculateAllPitches()
    }

    private fun calculateAllPitches() {
        pitches.clear()
        referenceNote.pitch = referencePitch
        // using Linked List because we treat it as a queue
        assignPitchesFromNote(referenceNote, LinkedList(), HashSet())
    }

    private fun assignPitchesFromNote(fromNote: Note, todo: MutableList<Relationship>, done: MutableSet<Relationship>) {
        val relationshipsFromNote = destinationsFrom(fromNote).map {
            (toNote, temper) -> Relationship(fromNote, toNote, temper)
        }
        todo.addAll(relationshipsFromNote)
        assignPitchesFromRelationships(todo, done)
    }

    private fun assignPitchesFromRelationships(
            todo: MutableList<Relationship>, done: MutableSet<Relationship>
    ) {
        if (todo.isEmpty()) return

        val rel = todo.removeAt(0)
        if (!done.contains(rel)) {
            done.add(rel)

            updatePitch(rel.fromNote, rel.toNote, rel.temper)
            assignPitchesFromNote(rel.fromNote, todo, done)
            assignPitchesFromNote(rel.toNote, todo, done)
        }
    }

    private var Note.pitch: Hertz?
        get() = pitches[this]
        set(freq) { pitches[this] = if (freq == null) null else normalize(freq) }

    private fun Note.hasPitch(): Boolean = pitches.containsKey(this)

    private fun Hertz.equalsWithinTolerance(other: Hertz): Boolean {
        return abs(this - other) < HERTZ_TOLERANCE
    }

    private data class Destination(val note: Note, val temper: Temper)
}
