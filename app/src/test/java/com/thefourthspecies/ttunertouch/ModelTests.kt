package com.thefourthspecies.ttunertouch

import org.junit.Test

import org.junit.Assert.*

/**
 * Created by Graham on 2018-01-06.
 */
class ModelTests {

    @Test
    fun intervalTests() {
        val n = Note(Note.Letter.A)
        val nn = n.relations()
        val actualNotes = HashSet<Note>(nn)
        val expectedNotes = mutableSetOf<Note>(
                Note(Note.Letter.C),
                Note(Note.Letter.C, Note.Accidental.SHARP),
                Note(Note.Letter.D),
                Note(Note.Letter.E),
                Note(Note.Letter.F),
                Note(Note.Letter.F, Note.Accidental.SHARP)
        )
        assertEquals(expectedNotes, actualNotes)
    }

    @Test
    fun temperamentTests() {

    }

}
