package com.thefourthspecies.ttunertouch

import android.util.Log

enum class Comma(ratio: Double, val symbol: String) {
    PYTHAGOREAN(531441.0/524288.0, "P"), // (12*P5)/(7*P8) = ((3/2)^12)/(2^7) = (3^12)/(2^19)
    SYNTONIC(81.0/80.0, "S"), // (4*P5)/(1*M3+2*P8) = (3/2)^4/(5/4 * 2^2) = (3^4)/((2^4)*5)
    ENHARMONIC(128.0/125.0, "E"), // P8/3*M3 = (2^12)/((5/4)^3) = (2^7)/(5^3)
    PURE(1.0, "=")
}
data class Temper(val interval: Interval, val fraction: Double, val comma: Comma)

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

typealias Hertz = Double

class Temperament(referencePitch: Hertz, referenceNote: Note) {
    var referencePitch: Hertz = referencePitch
        set(freq) {
            field = freq
            invalidate()
        }
    var referenceNote: Note = referenceNote
        set(note) {
            field = note
            invalidate()
        }

    private val pitches = HashMap<Note, Hertz?>()

    private val relationshipGraph = HashMap<Note, MutableList<Destination>>()
    val relationships: List<Relationship>
        get() {
            val relSet = mutableSetOf<Relationship>()
            val rels: List<Relationship> = relationshipGraph.flatMap {
                (note, dests) -> dests.map { Relationship(note, it.note, it.temper)  }
            }
            return rels
        }

    private data class Destination(val note: Note, val temper: Temper)

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

    private fun destinationsFrom(note: Note): MutableList<Destination> {
        return relationshipGraph[note] ?: mutableListOf<Destination>()
    }

    private fun removeReferences(fromNote: Note, toNote: Note) {
        destinationsFrom(fromNote).removeAll {
            it.note == toNote
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

    private fun updatePitch(from: Note, to: Note, temper: Temper) {
        when {
            from.hasPitch() && to.hasPitch() -> updatePitchFromBase(from, to, temper)
            from.hasPitch() -> updatePitchFromBase(from, to, temper)
            to.hasPitch() -> updatePitchFromBase(to, from, temper)
            else -> {/*neither has a pitch, so nothing to update*/}
        }
    }

    private fun updatePitchFromBase(base: Note, dest: Note, temper: Temper) {
        val basePitch = base.pitch()
        if (basePitch != null) {
            val direction = intervalDirection(base, dest, temper.interval)
            pitches[dest] = calculateTemperedPitch(basePitch, direction, temper)
        } else {
            Log.d(DEBUG_TAG,
                    "${::updatePitchFromBase.name}: " +
                            "Invalidating Temperament pitches unexpectedly because base pitch is null." +
                            " base=$base, dest=$dest, temper=$temper")
            invalidate()
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
        // todo
        return 0.0
    }


    private fun invalidate() {
        pitches.clear()
        calculateAllPitches()
    }

    private fun calculateAllPitches() {
        pitches[referenceNote] = referencePitch
        // todo
    }

    fun Note.pitch(): Hertz? = pitches[this]
    fun Note.hasPitch(): Boolean = pitches.containsKey(this)
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