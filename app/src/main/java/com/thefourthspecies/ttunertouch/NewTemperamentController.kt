package com.thefourthspecies.ttunertouch

import android.provider.SyncStateContract.Helpers.update

val DEFAULT_TOP_NOTE = Note(Note.Letter.C)
val DEFAULT_REFERENCE_NOTE = Note(Note.Letter.A)
val DEFAULT_REFERENCE_PITCH = 415.0


interface TemperamentController {
    val temperament: Temperament
    val uiNotes: Set<NoteCircle.UINote>
    val uiRelationships: Set<NoteCircle.UIRelationship>
}

/**
 * Created by Graham on 2018-02-21.
 */
class NewTemperamentController(val noteCircle: NoteCircle) : TemperamentController {
    override val temperament = ChromaticTemperament(DEFAULT_REFERENCE_NOTE, DEFAULT_REFERENCE_PITCH)

    override val uiRelationships: Set<NoteCircle.UIRelationship>
        get() = temperament.relationships.map { it.toUI() }.toSet()
    override val uiNotes: Set<NoteCircle.UINote>
        get() = temperament.notes.map { it.toUI() }.toSet()

    var topNote = DEFAULT_TOP_NOTE
        set(note) {
            field = note
            update()
        }


    val defaultNotes = mutableListOf(
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
        )

    init {
        update()
    }

    private fun update() {
        noteCircle.update(this)
    }


    private fun defaultPitch(note: Note): Hertz {
        // TODO TEST?
        val numSemiTones = note chromaticMinus temperament.referenceNote
        val ratio = Math.pow(2.0, numSemiTones / CHROM_SIZE.toDouble())
        return temperament.referencePitch * ratio

    }

    private fun calculatePosition(notePitch: Hertz): Double {
        // TODO TEST?
        val ratio: Double = notePitch / topNote.pitch
        return Math.log(ratio) / Math.log(2.0)
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
        val position = calculatePosition(this.pitch)
        return noteCircle.UINote(position, this.name, isHint)
    }

    fun Relationship.toUI(): NoteCircle.UIRelationship {
        val isArc = temper.comma != Comma.PURE
        return noteCircle.UIRelationship(fromNote.toUI(), toNote.toUI(), temper.label, isArc)
    }

}


