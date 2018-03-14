package com.thefourthspecies.ttunertouch

import android.util.Log

val DEFAULT_TOP_NOTE = Note(Note.Letter.C)
val DEFAULT_REFERENCE_NOTE = Note(Note.Letter.A)
val DEFAULT_REFERENCE_PITCH = 415.0

enum class Order {
    FIFTHS,
    PITCH;

    companion object {
        const val NUM_FIFTHS = 12
    }
}

data class TouchIn(val point: Point, val fromNote: Note, val toNote: Note?, val sweepAngle: Float)
data class TouchOut(val input: TouchIn, val isArc: Boolean, val highlighted: List<Note>)


interface TemperamentController {
    val temperament: Temperament
    val uiNotes: Set<NoteCircle.UINote>
    val uiRelationships: Set<NoteCircle.UIRelationship>

    fun input(input: TouchIn)
}

/**
 * Created by Graham on 2018-02-21.
 */
class NewTemperamentController(val noteCircle: NoteCircle) : TemperamentController {
    override val temperament = ChromaticTemperament(DEFAULT_REFERENCE_NOTE, DEFAULT_REFERENCE_PITCH)

    override val uiRelationships: Set<NoteCircle.UIRelationship>
        get() = temperament.relationships.map { it.toUI() }.toSet()
    override val uiNotes: Set<NoteCircle.UINote>
        get() {
            val allNotes = temperament.notes + defaultNotes
            return allNotes.map { it.toUI() }.toSet()
        }

    var topNote = DEFAULT_TOP_NOTE
        set(note) {
            field = note
            update()
        }


    val defaultNotes: MutableSet<Note> = (setOf(
            Note(Note.Letter.A, Note.Accidental.FLAT),
            Note(Note.Letter.E, Note.Accidental.FLAT),
            Note(Note.Letter.B, Note.Accidental.FLAT),
            Note(Note.Letter.F),
            Note(Note.Letter.C),
            Note(Note.Letter.G),
            Note(Note.Letter.D),
            Note(Note.Letter.A),
            Note(Note.Letter.E),
            Note(Note.Letter.B),
            Note(Note.Letter.F, Note.Accidental.SHARP),
            Note(Note.Letter.C, Note.Accidental.SHARP)
        ) - DEFAULT_REFERENCE_NOTE).toMutableSet()

    var order = Order.FIFTHS

    init {
        noteCircle.controller = this
        update()
    }

    override fun input(input: TouchIn) {
        Log.d(DEBUG_TAG, "Inputting touch event: $input")

        // TODO
        val isArc = true
        val highlighted = emptyList<Note>()
        // TODO
        val output = TouchOut(input, isArc, highlighted)
        noteCircle.update(output)
//
//        val fromNote = input.fromNote
//        val toNote = input.toNote
//        if (fromNote != null && toNote != null) {
//            val interval = fromNote.intervalTo(toNote)
//            if (interval != null) {
//                val temper = if (input.isDirect)
//                    Temper(interval)
//                else
//                    Temper(interval, Comma.PYTHAGOREAN, Fraction(1, 6), Temper.Change.SMALLER)
//
//                // Do it:
//                Log.d(DEBUG_TAG, "Setting relationship: fromNote=$fromNote, toNote=$toNote, temper=$temper")
//
//                defaultNotes.remove(fromNote)
//                defaultNotes.remove(toNote)
//                temperament.setRelationship(fromNote, toNote, temper)
//                update()
//            }
//
//        }

    }


    private fun update() {
        noteCircle.update(uiNotes.toList(), uiRelationships.toList())
    }


    private fun defaultPitch(note: Note): Hertz {
        // TODO TEST?
        val numSemiTones = note chromaticMinus temperament.referenceNote
        val ratio = Math.pow(2.0, numSemiTones / CHROM_SIZE.toDouble())
        return temperament.referencePitch * ratio

    }

    val Note.fifthIndex: Int
        get() {
            val semis: Int = this chromaticMinus topNote
            return when (semis) {
                0  -> 0
                7  -> 1
                2  -> 2
                9  -> 3
                4  -> 4
                11 -> 5
                6  -> 6
                1  -> 7
                8  -> 8
                3  -> 9
                10 -> 10
                5  -> 11
                else -> throw AssertionError(
                        "fifthIndex is greater than 11: Note: $this, semis=$semis")
            }
        }

    val Note.position: Double
        get() = when (order) {
            Order.FIFTHS -> {
                val i = fifthIndex
                i.toDouble() / Order.NUM_FIFTHS
            }
            Order.PITCH -> {
                val ratio: Double = this.pitch / topNote.pitch
                Math.log(ratio) / Math.log(2.0)
            }
        }

    val Note.pitch: Hertz
        get() = temperament.pitchOf(this) ?: defaultPitch(this)


    fun Note.toUI(): NoteCircle.UINote {
        val isHint = defaultNotes.contains(this)
        if (isHint) {
            assert(!temperament.notes.contains(this)) {
                "Note exists in default form as well as in Temperament: $this"
            }
        }
        return noteCircle.UINote(position, this, isHint)
    }

    fun Relationship.toUI(): NoteCircle.UIRelationship {
        val isArc = temper.comma != Comma.PURE
        return noteCircle.UIRelationship(fromNote.toUI(), toNote.toUI(), temper.label, isArc)
    }





}


