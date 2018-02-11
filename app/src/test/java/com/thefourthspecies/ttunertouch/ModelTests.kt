package com.thefourthspecies.ttunertouch

import org.junit.Test

import org.junit.Assert.*

/**
 * Created by Graham on 2018-01-06.
 */
class ModelTests {

    @Test
    fun intervalTests() {
        val n = Note(Letter.A)
        val nn = n.relations()
        val actualNotes = HashSet<Note>(nn)
        val expectedNotes = mutableSetOf<Note>(
                Note(Letter.C),
                Note(Letter.C, Accidental.SHARP),
                Note(Letter.D),
                Note(Letter.E),
                Note(Letter.F),
                Note(Letter.F, Accidental.SHARP)
        )
        assertEquals(expectedNotes, actualNotes)
    }

}
