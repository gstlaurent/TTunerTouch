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
// Tempers that have tempered ratios that calculate out to the same value are essentially the same, and considered equal
data class Temper(val interval: Interval, val fraction: Double, val comma: Comma) {
    val temperedRatio: Double = interval.ratio * commaFractionRatio

    private val commaFractionRatio: Double
        get() = Math.pow(comma.ratio, fraction)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Temper) return false

        if (temperedRatio != other.temperedRatio) return false

        return true
    }

    override fun toString(): String = "Temper($interval, %.4f, $comma): %.4f".format(fraction, temperedRatio)
}

// A Relationship is like an undirected Edge, so equivalent Relationships can have fromNote and toNote reversed
data class Relationship(val fromNote: Note, val toNote: Note, val temper: Temper) {

    fun isBetween(note1: Note, note2: Note): Boolean {
        return  (note1 == fromNote && note2 == toNote) ||
                (note2 == fromNote && note1 == toNote)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Relationship) return false

        if (temper != other.temper) return false
        if (!isBetween(other.fromNote, other.toNote)) return false

        return true
    }

    override fun hashCode(): Int {
        val note1 = minOf(fromNote, toNote)
        val note2 = maxOf(fromNote, toNote)

        var result = temper.hashCode()
        result = 31 * result + note1.hashCode()
        result = 31 * result + note2.hashCode()
        return result
    }

    override fun toString(): String {
        return "Relationship($fromNote -> $toNote, $temper)"
    }
}

interface Temperament {
    var referenceNote: Note
    var referencePitch: Hertz
    val relationships: List<Relationship>
    val notes: List<Note>

    fun pitchOf(note: Note): Hertz?
    fun addNote(note: Note)
    fun removeNote(note: Note)
    fun setRelationship(from: Note, to: Note, temper: Temper)
    fun clearRelationship(note1: Note, note2: Note)
}

open class PureTemperament(referenceNote: Note, referencePitch: Hertz) : Temperament {
    private val relationshipGraph = HashMap<Note, MutableList<Relationship>>()
    private val pitches = HashMap<Note, Hertz?>()

    override var referenceNote: Note = referenceNote
        set(note) {
            field = note
            invalidate()
        }

    override var referencePitch: Hertz = referencePitch
        set(freq) {
            field = freq
            invalidate()
        }

    init {
        assert(referencePitch > 0.0) {
            "Reference Pitch must be greater than 0. Given $referencePitch"
        }
        invalidate()
    }

    override val relationships: List<Relationship>
        get() = relationshipGraph.values.flatten().toSet().toList()

    override val notes: List<Note>
        get() = relationshipGraph.keys.toList()

    override fun pitchOf(note: Note): Hertz? = note.pitch

    override fun addNote(note: Note) {
        if (!relationshipGraph.containsKey(note)) {
            relationshipGraph[note] = mutableListOf()
        }
    }

    override fun removeNote(note: Note) {
        if (!relationshipGraph.contains(note)) return

        val rels = note.relationships
        relationshipGraph.remove(note)

        for ((_, toNote, _) in rels) {
            removeReferences(toNote, note)
        }

        pitches.remove(note)
    }

    override fun setRelationship(from: Note, to: Note, temper: Temper) {
        assertIntervalApplies(from, to, temper.interval)

        replaceRelationship(from, to, temper)

        // To determine if there are any conflicting relationships that need to be removed
        updatePitch(from, to, temper)

        // To find any notes that now have a path to the reference note, and assign them pitches
        invalidate()
    }

    private fun assertIntervalApplies(from: Note, to: Note, interval: Interval) {
        intervalDirectionNotNull(from, to, interval)
    }

    override fun clearRelationship(note1: Note, note2: Note) {
        removeRelationship(note1, note2)
        invalidate()
    }

    private fun replaceRelationship(fromNote: Note, toNote: Note, temper: Temper) {
        removeRelationship(fromNote, toNote)
        fromNote.relationships.add(Relationship(fromNote, toNote, temper))
        toNote.relationships.add(Relationship(toNote, fromNote, temper))
    }

    private fun removeRelationship(note1: Note, note2: Note) {
        removeReferences(note1, note2)
        removeReferences(note2, note1)
    }

    private fun removeReferences(fromNote: Note, toNote: Note) {
        if (relationshipGraph.containsKey(fromNote)) {
            fromNote.relationships.removeAll {
                it.toNote == toNote
            }
        }
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
            val direction = intervalDirectionNotNull(base, dest, temper.interval)
            val pitch = calculateTemperedPitch(basePitch, direction, temper)
            deleteConflictingRelationships(dest, pitch, base)
            dest.pitch = pitch
        } else {
            Log.d(DEBUG_TAG,
                    "${::updatePitchFromBase.name}: " +
                            "Temperament pitches inconsistent because base pitch is null." +
                            " base=$base, dest=$dest, temper=$temper")
        }
    }

   private fun intervalDirectionNotNull(from: Note, to: Note, interval: Interval): Direction {
       return intervalDirection(from, to , interval) ?: throw Exception("Interval does not apply for notes: from=$from, to=$to, interval=$interval")
   }

    protected open fun intervalDirection(from: Note, to: Note, interval: Interval): Direction? {
        return when {
            from.atIntervalAbove(interval) == to -> Direction.ASCENDING
            to.atIntervalAbove(interval) == from -> Direction.DESCENDING
            else -> null
        }
    }

    // Delete relationships not from goodNeighbour, if pitches don't match
    private fun deleteConflictingRelationships(note: Note, goodPitch: Hertz, goodNeighbour: Note) {
        val prevPitch = note.pitch
        if (prevPitch != null && !prevPitch.equalsWithinTolerance(goodPitch)) {
            val conflicts = note.relationships.filter { it.toNote != goodNeighbour }
            for ((_, conflictNote, _) in conflicts) {
                removeRelationship(note, conflictNote)
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
        todo.addAll(fromNote.relationships)
        assignPitchesFromRelationships(todo, done)
    }

    private fun assignPitchesFromRelationships(
            todo: MutableList<Relationship>, done: MutableSet<Relationship>
    ) {
        if (todo.isEmpty()) return

        val rel = todo.removeAt(0)
        if (done.contains(rel)) {
            assignPitchesFromRelationships(todo, done)
        } else {
            done.add(rel)

            updatePitch(rel.fromNote, rel.toNote, rel.temper)
            assignPitchesFromNote(rel.toNote, todo, done)
        }
    }

    private var Note.pitch: Hertz?
        get() = pitches[this]
        set(freq) { pitches[this] = if (freq == null) null else normalize(freq) }

    private fun Note.hasPitch(): Boolean = pitches.containsKey(this)

    private val Note.relationships: MutableList<Relationship>
        get() = relationshipGraph.getOrPut(this) { mutableListOf() }

    private fun Hertz.equalsWithinTolerance(other: Hertz): Boolean {
        return abs(this - other) < HERTZ_TOLERANCE
    }
}

class ChromaticTemperament(referenceNote: Note, referencePitch: Hertz) : PureTemperament(referenceNote, referencePitch) {
    override fun intervalDirection(from: Note, to: Note, interval: Interval): Direction? {
        return when (interval.chromaticDifference) {
            to chromaticMinus from -> Direction.ASCENDING
            from chromaticMinus to -> Direction.DESCENDING
            else -> null
        }
    }
}

