package com.thefourthspecies.ttunertouch

import android.util.Log
import java.text.Normalizer.normalize
import kotlin.math.abs

const val LOW_FREQ = 400.0
const val HIGH_FREQ = 800.0
const val HERTZ_TOLERANCE = 10e-10  // Between 400 and 800, experimenting shows me that Doubles seem
                                    // to be to accurate to about 10e-12.
                                    // Remove off two places for safety.

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
        get() {
            val magnitude = abs(comma.ratio * fraction)
            return when {
                fraction > 0.0 -> magnitude
                fraction < 0.0 -> 1.0 / magnitude
                else -> 1.0
            }
        }
}

/**
 * Two Relationships are considered equal if they contain the same notes, whether they are note2 or note1.
 */
class Relationship(note1: Note, note2: Note, val temper: Temper) {
    val note1 = minOf(note1, note2)
    val note2 = maxOf(note1, note2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Relationship) return false

        if (note1 != other.note1) return false
        if (note2 != other.note2) return false
        if (temper != other.temper) return false

        return true
    }

    override fun hashCode(): Int {
        var result = note1.hashCode()
        result = 31 * result + note2.hashCode()
        result = 31 * result + temper.hashCode()
        return result
    }

    override fun toString(): String {
        return "Relationship(temper=$temper, note1=$note1, note2=$note2)"
    }
}


class Temperament(referencePitch: Hertz, referenceNote: Note) {
    private val relationshipGraph = HashMap<Note, MutableList<Destination>>()
    private val pitches = HashMap<Note, Hertz?>()

    var referencePitch: Hertz = referencePitch
        set(freq) {
            field = normalize(freq)
            invalidate()
        }

    var referenceNote: Note = referenceNote
        set(note) {
            field = note
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
    }

    fun setRelationship(from: Note, to: Note, temper: Temper) {
        removeRelationship(from, to)
        destinationsFrom(from).add(Destination(to, temper))
        destinationsFrom(to).add(Destination(from, temper))

        updatePitch(from, to, temper)
    }

    fun removeRelationship(note1: Note, note2: Note) {
        removeReferences(note1, note2)
        removeReferences(note2, note1)
    }

    private fun removeReferences(fromNote: Note, toNote: Note) {
        destinationsFrom(fromNote).removeAll {
            it.note == toNote
        }
    }

    private fun destinationsFrom(note: Note): MutableList<Destination> {
        return relationshipGraph[note] ?: mutableListOf<Destination>()
    }

    private fun updatePitch(from: Note, to: Note, temper: Temper) {
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
            val direction = intervalDirection(base, dest, temper.interval)
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

    private fun intervalDirection(from: Note, to: Note, interval: Interval): Direction {
        return when {
            from.atIntervalAbove(interval) == to -> Direction.ASCENDING
            to.atIntervalAbove(interval) == from -> Direction.DESCENDING
            else -> throw Exception("Interval does not apply for notes: from=$from, to=$to, interval=$interval")
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
        assignPitchesFrom(referenceNote)
    }

    private fun assignPitchesFrom(startNote: Note) {
        for (dest in destinationsFrom(startNote)) {
            if (!dest.note.hasPitch()) {
                updatePitch(startNote, dest.note, dest.temper)
                assignPitchesFrom(dest.note)
            }
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

///**
// * Created by Graham on 2018-01-04.

// This only works if Relationship doesn't use Temper in hashCode or Equals
// */
//class Temperament {
//    private var _notes: HashSet<Note> = HashSet()
//    val notes: List<Note>
//        get() = _notes.toList()
//
//    private var _relationships: HashSet<Relationship> = HashSet()
//    val relationships: List<Relationship>
//        get() = _relationships.toList()
//
//
//    fun addNote(note: Note) {
//        _notes.add(note)
//    }
//
//    fun removeNote(note: Note) {
//        _notes.remove(note)
//
//        val iterate = _relationships.iterator()
//        while (iterate.hasNext()) {
//            val rel = iterate.next()
//            if (rel.note1 == note || rel.note2 == note) {
//                iterate.remove()
//            }
//        }
//    }
//
//    fun setRelationship(relationship: Relationship) {
//        _relationships.remove(relationship)
//        _relationships.add(relationship)
//    }
//
//    fun deleteRelationship(relationship: Relationship) {
//        _relationships.remove(relationship)
//    }
//}