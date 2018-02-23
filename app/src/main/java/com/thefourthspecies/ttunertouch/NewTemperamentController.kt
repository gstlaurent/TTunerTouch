package com.thefourthspecies.ttunertouch

/**
 * Created by Graham on 2018-02-21.
 */
class NewTemperamentController(val temperament: Temperament, val noteCircle: NoteCircle) {
    val defaultNotes = mutableListOf<Note>(
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

    fun Note.toUI(): NoteCircle.UINote {
        // TODO
        return noteCircle.UINote(0.0, "")
    }

    fun Relationship.toUI(): NoteCircle.UIRelationship {
        // TODO
        return noteCircle.UIRelationship(defaultNotes[0].toUI(), defaultNotes[1].toUI(),"", false  )
    }

}


