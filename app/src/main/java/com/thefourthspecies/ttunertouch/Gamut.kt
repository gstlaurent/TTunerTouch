package com.thefourthspecies.ttunertouch

/**
 * Created by Graham on 2018-01-03.
 */
class Gamut {
    companion object {
        const val CHROM_SIZE = 12
    }
    /**
     * The names of the white base of each note
     * The white offset is the number of white keys from A
     * The offset is the number of semitones from A
     */
    enum class Letter(val whiteOffset: Int, val chromaticOffset: Int) {
        A(0, 0),
        B(1, 2),
        C(2, 3),
        D(3, 5),
        E(4, 7),
        F(5, 8),
        G(6,10)
    }

    enum class Accidental(val symbol: String) {
        FLAT("♭") {
            override fun parity() = -1
        }, // U+266D
        SHARP("♯") {
            override fun parity() = 1
        }, // U+266F
        NONE("") {
            override fun parity() = 0
        };

        abstract fun parity(): Int
    }

    /**
     * All temperable intervals
     */
    enum class Interval(val whiteDifference: Int, val chromaticDifference: Int) {
        MINOR_THIRD(2, 3),
        MAJOR_THIRD(2, 4),
        PERFECT_FOURTH(3, 5),
        PERFECT_FIFTH(4, 7),
        MINOR_SIXTH(5, 8),
        MAJOR_SIXTH(5, 9)
    }

    data class Note(val letter: Letter, val accidental: Accidental, val numAccidental: Int)


    /**
     * Returns a list of all Notes that are an temperable Interval away from the given Note
     */
    fun relations(note: Note): List<Note> {
        // TODO
        return listOf(note)

    }

    fun letterAtOffset(offset: Int): Letter {
        val o = offset % Letter.values().size
        val letter = Letter.values().find { it.whiteOffset == o }
        return letter!!
    }

    fun chromaticMod(offset: Int): Int {
        val o = if (offset < 0) {
           offset + CHROM_SIZE
        } else {
            offset
        }
        return o % CHROM_SIZE
    }

    fun accidentalAdjustment(note: Note): Int {
        return note.accidental.parity() * note.numAccidental
    }

    fun atIntervalAbove(note: Note, interval: Interval): Note {
        val upperLetter = letterAtOffset(note.letter.whiteOffset + interval.whiteDifference)
        val adjustedChromaticOffset = note.letter.chromaticOffset + accidentalAdjustment(note)
        val upperWhiteDifference = chromaticMod(upperLetter.chromaticOffset - adjustedChromaticOffset)
        val numAccidental = interval.chromaticDifference - upperWhiteDifference

        val upperAccidental = when {
            numAccidental > 0 -> Accidental.SHARP // must widen interval
            numAccidental < 0 -> Accidental.FLAT // must shrink interval
            else -> Accidental.NONE
        }
        val outNote = Note(upperLetter, upperAccidental, Math.abs(numAccidental))
        return outNote
    }

}