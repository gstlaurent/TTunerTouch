package com.thefourthspecies.ttunertouch

import org.junit.Test

import org.junit.Assert.*

/**
 * Created by Graham on 2018-01-06.
 */
class ModelTests {
    val Af =  Note(Note.Letter.A, Note.Accidental.FLAT)
    val Ef =  Note(Note.Letter.E, Note.Accidental.FLAT)
    val Bf =  Note(Note.Letter.B, Note.Accidental.FLAT)
    val F =   Note(Note.Letter.F)
    val C =   Note(Note.Letter.C)
    val G =   Note(Note.Letter.G)
    val D =   Note(Note.Letter.D)
    val A =   Note(Note.Letter.A)
    val E =   Note(Note.Letter.E)
    val B =   Note(Note.Letter.B)
    val Fs =  Note(Note.Letter.F, Note.Accidental.SHARP)
    val Cs =  Note(Note.Letter.C, Note.Accidental.SHARP)


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
        val pitchA = 415.0
        val eqTemp = Temper(Interval.PERFECT_FIFTH, 1.0/12.0, Comma.PYTHAGOREAN)
        val t = Temperament(A, pitchA)
        t.setRelationship(Af, Ef, eqTemp)
        t.setRelationship(Ef, Bf, eqTemp)
        t.setRelationship(Bf, F,  eqTemp)
        t.setRelationship(F,  C,  eqTemp)
        t.setRelationship(C,  G,  eqTemp)
        t.setRelationship(G,  D,  eqTemp)
        t.setRelationship(D,  A,  eqTemp)
        t.setRelationship(A,  E,  eqTemp)
        t.setRelationship(E,  B,  eqTemp)
        t.setRelationship(B,  Fs, eqTemp)
        t.setRelationship(Fs, Cs, eqTemp)
//        t.setRelationship(Cs, Af, eqTemp)

        val notes = setOf<Note>(Af, Ef, Bf, F, C, G, D, A, E, B, Fs, Cs)
        assertEquals(notes, t.notes.toSet())

        assertEquals(t.pitchOf(Af)!!, pitchA*Math.pow(2.0, 5/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(Ef)!!, pitchA*Math.pow(2.0, 6/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(Bf)!!, pitchA*Math.pow(2.0, 7/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(F)!!,  pitchA*Math.pow(2.0, 8/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(C)!!,  pitchA*Math.pow(2.0, 9/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(G)!!,  pitchA*Math.pow(2.0, 10/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(D)!!,  pitchA*Math.pow(2.0, 11/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(A)!!,  pitchA*Math.pow(2.0, 0/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(E)!!,  pitchA*Math.pow(2.0, 1/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(B)!!,  pitchA*Math.pow(2.0, 2/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(Fs)!!, pitchA*Math.pow(2.0, 3/12.0), HERTZ_TOLERANCE)
        assertEquals(t.pitchOf(Cs)!!, pitchA*Math.pow(2.0, 4/12.0), HERTZ_TOLERANCE)

        assertTrue(t.relationships.contains(Relationship(Ef, Af, eqTemp)))
        assertTrue(t.relationships.contains(Relationship(Cs, Af, eqTemp)))
    }


}
