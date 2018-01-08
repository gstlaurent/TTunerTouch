package com.thefourthspecies.ttunertouch

enum class Comma(ratio: Double, val symbol: String) {
    PYTHAGOREAN(531441.0/524288.0, "P"), // (12*P5)/(7*P8) = ((3/2)^12)/(2^7) = (3^12)/(2^19)
    SYNTONIC(81.0/80.0, "S"), // (4*P5)/(1*M3+2*P8) = (3/2)^4/(5/4 * 2^2) = (3^4)/((2^4)*5)
    ENHARMONIC(128.0/125.0, "E"), // P8/3*M3 = (2^12)/((5/4)^3) = (2^7)/(5^3)
    PURE(1.0, "=")
}
data class Temper(val numerator: Int, val denominator: Int, val comma: Comma)

/**
 * Two Relationships are considered equal if they contain the same notes, whether they are note2 or note1,
 * and regardless of the Temper
 */
class Relationship(note1: Note, note2: Note, val temper: Temper) {
    val note1 = minOf(note1, note2)
    val note2 = maxOf(note1, note2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Relationship) return false

        if (note1 != other.note1) return false
        if (note2 != other.note2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = note1.hashCode()
        result = 31 * result + note2.hashCode()
        return result
    }

    override fun toString(): String {
        return "Relationship(temper=$temper, note1=$note1, note2=$note2)"
    }
}

/**
 * Created by Graham on 2018-01-04.
 */
class Temperament {
    private var _notes: HashSet<Note> = HashSet()
    val notes: List<Note>
        get() = _notes.toList()

    private var _relationships: HashSet<Relationship> = HashSet()
    val relationships: List<Relationship>
        get() = _relationships.toList()


    fun addNote(note: Note) {
        _notes.add(note)
    }

    fun removeNote(note: Note) {
        _notes.remove(note)

        val iterate = _relationships.iterator()
        while (iterate.hasNext()) {
            val rel = iterate.next()
            if (rel.note1 == note || rel.note2 == note) {
                iterate.remove()
            }
        }
    }

    fun setRelationship(relationship: Relationship) {
        _relationships.remove(relationship)
        _relationships.add(relationship)
    }

    fun deleteRelationship(relationship: Relationship) {
        _relationships.remove(relationship)
    }
}