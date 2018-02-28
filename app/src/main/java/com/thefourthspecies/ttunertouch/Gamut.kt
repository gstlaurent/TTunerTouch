package com.thefourthspecies.ttunertouch

const val CHROM_SIZE = 12

/**
 * Represents a note. Notes can have zero, one, or more accidentals following its name, but they all must be
 * the same type of accidental. (E.g. F# or Bbb, but not F#b)
 */
class Note(val letter: Letter, accidental: Accidental = Accidental.NONE, numAccidental: Int = 1) : Comparable<Note> {
    val accidental = if (numAccidental == 0) Accidental.NONE else accidental
    val numAccidental = if (accidental == Accidental.NONE) 0 else numAccidental

    val accidentalNumeric: Int = accidental.parity * numAccidental
    val chromaticOffset: Int = letter.chromaticOffset + accidentalNumeric

    val name: String = letter.name + accidental.symbol.repeat(numAccidental)

    infix fun chromaticMinus(n: Note): Int = chromaticMod(chromaticOffset - n.chromaticOffset)

    /**
     * Returns a list of all Notes that are a temperable Interval away from the given Note
     */
    fun relations(): List<Note> {
        return Interval.values().map { atIntervalAbove(it) }
    }

    fun intervalTo(note: Note): Interval? {
        return Interval.values().find {
            this.atIntervalAbove(it) == note || note.atIntervalAbove(it) == this
        }
    }


    fun atIntervalAbove(interval: Interval): Note {
        val upperLetter = letterAtOffset(letter.diatonicOffset + interval.diatonicDifference)
        val adjustedChromaticOffset = letter.chromaticOffset + accidentalNumeric
        val upperDiatonicDifference = chromaticMod(upperLetter.chromaticOffset - adjustedChromaticOffset)
        val numAccidental = interval.chromaticDifference - upperDiatonicDifference

        val upperAccidental = when {
            numAccidental > 0 -> Accidental.SHARP // must widen interval
            numAccidental < 0 -> Accidental.FLAT // must shrink interval
            else -> Accidental.NONE
        }
        val outNote = Note(upperLetter, upperAccidental, Math.abs(numAccidental))
        return outNote
    }

    /**
     * Returns the letter that has the given diatonic offset
     */
    private fun letterAtOffset(offset: Int): Letter {
        val o = offset % Letter.values().size
        val letter = Letter.values().find { it.diatonicOffset == o }
        return letter!!
    }

    /**
     * Returns the offset in semitones, from 0-11.
     */
    private fun chromaticMod(offset: Int): Int {
        val o = if (offset < 0) {
            offset + CHROM_SIZE
        } else {
            offset
        }
        return o % CHROM_SIZE
    }

    override operator fun compareTo(other: Note): Int {
        val result = when {
            letter > other.letter -> 1
            letter < other.letter -> -1
            else -> accidentalNumeric.compareTo(other.accidentalNumeric)
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Note) return false

        if (letter != other.letter) return false
        if (accidental != other.accidental) return false
        if (numAccidental != other.numAccidental) return false

        return true
    }

    override fun hashCode(): Int {
        var result = letter.hashCode()
        result = 31 * result + accidental.hashCode()
        result = 31 * result + numAccidental
        return result
    }

    override fun toString() = "Note($name)"

    /**
     * The names of the white base of each note
     * The white offset is the number of white keys from A
     * The offset is the number of semitones from A
     */
    enum class Letter(val diatonicOffset: Int, val chromaticOffset: Int) {
        A(0, 0),
        B(1, 2),
        C(2, 3),
        D(3, 5),
        E(4, 7),
        F(5, 8),
        G(6,10)
    }

    enum class Accidental(val symbol: String, val parity: Int) {
        FLAT("♭", -1),// U+266D
        SHARP("♯", 1), // U+266F
        NONE("", 0)
    }
}


/**
 * All temperable intervals, minus inversions.
 * @diatonicDifference: the distance in letter names (white keys)
 * @chromaticDifference: the distance in semitones
 * @ratio: note2 note frequency to note1 note frequency
 */
enum class Interval(val diatonicDifference: Int, val chromaticDifference: Int, val ratio: Double) {
    MINOR_THIRD(2, 3, 6.0/5.0),
    MAJOR_THIRD(2, 4, 5.0/4.0),
    PERFECT_FOURTH(3, 5, 4/3.0),
    PERFECT_FIFTH(4, 7, 3.0/2.0),
    MINOR_SIXTH(5, 8, 5.0/3.0),
    MAJOR_SIXTH(5, 9, 8.0/5.0)
}

