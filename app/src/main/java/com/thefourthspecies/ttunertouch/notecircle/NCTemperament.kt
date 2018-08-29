package com.thefourthspecies.ttunertouch.notecircle

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.thefourthspecies.ttunertouch.model.*
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG
import com.thefourthspecies.ttunertouch.util.RingList
import kotlin.properties.Delegates

internal data class NCTemperament(var relationships: Set<NCRelationship> = emptySet(),
                         var notes: RingList<NCNote> = RingList<NCNote>()
) {
    companion object {
        fun createFrom(temperament: Temperament, view: NoteCircleView, order: Order, topNote: Note): NCTemperament {
            val notes: Map<Note, NCNote> = createNCNotes(temperament, view, order, topNote)
            val relationships: Set<NCRelationship> = createNCRelationships(temperament, notes)
            val ncTemperament = NCTemperament(relationships, RingList(notes.values))
            return ncTemperament
        }
    }
}

internal fun createNCNotes(
    temperament: Temperament,
    view: NoteCircleView,
    order: Order,
    topNote: Note
) : Map<Note, NCNote> {
    return temperament.notes.associate{ it to NCNote.createFrom(it, view, order, topNote, temperament) }
}

internal fun createNCRelationships(temperament: Temperament, notes: Map<Note, NCNote>): Set<NCRelationship> {
    return temperament.relationships.map {
        val startNote = notes.getValue(it.fromNote)
        val endNote = notes.getValue(it.toNote)
        val label = it.temper.label
        val isArc = it.temper.comma != Comma.PURE
        NCRelationship(startNote, endNote, label, isArc)
    }.toSet()
}


internal data class NCRelationship(val startNote: NCNote, val endNote: NCNote, val label: String, val isArc: Boolean) {

    fun draw(canvas: Canvas, nc: NoteCircleView) {
        val start = startNote.dotPoint
        val end = endNote.dotPoint

        if (isArc) {
            nc.drawArc(canvas, start.screenAngle, end.screenAngle)
        } else {
            canvas.drawLine(start.x, start.y, end.x, end.y, nc.mLinePaint)
        }
    }
}

internal class NCNote(position: Position, val note: Note, var isHint: Boolean = false, val nc: NoteCircleView) : Comparable<NCNote> {
    val position: Position = normalizePosition(position)
    val name: String = note.name
    val dotPoint: Point = Point.polar(position, nc.mInnerRadius)
    val labelPoint: Point = Point.polar(position, nc.mLabelRadius)
    var button: NoteCircleView.NoteButton by Delegates.notNull<NoteCircleView.NoteButton>()

    companion object {
        fun createFrom(note: Note, view: NoteCircleView, order: Order, topNote: Note, temperament: Temperament) : NCNote {
//            val isHint = defaultNotes.contains(this)
//            if (isHint) {
//                com.thefourthspecies.ttunertouch.util.assert(!temperament.notes.contains(this)) {
//                    "Note exists in default form as well as in Temperament: $this"
//                }
//            }
            val position = calculateNotePosition(note, order, topNote, temperament)
            return NCNote(position, note, isHint = false, nc = view) // TODO deal with isHint
        }
    }

    override fun compareTo(other: NCNote): Int {
        var comp = position.compareTo(other.position)
        if (comp == 0) {
            comp = note.compareTo(other.note)
        }
        return comp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NCNote) return false

        if (note != other.note) return false
        return true
    }

    override fun hashCode(): Int {
        return note.hashCode()
    }

    override fun toString(): String {
        return "Note(position=$position, name='$name', isHint=$isHint)"
    }

    private fun defaultPaints(paint: Paint, hintPaint: Paint) = if (isHint) hintPaint else paint

    fun drawLabel(canvas: Canvas, paint: Paint = defaultPaints(nc.mLabelPaint, nc.mHintLabelPaint)) {
        nc.drawTextCentered(canvas, paint, name, labelPoint.x, labelPoint.y)
    }

    fun drawDot(canvas: Canvas, paint: Paint = defaultPaints(nc.mDotPaint, nc.mHintLabelPaint)) {
        canvas.drawCircle(dotPoint.x, dotPoint.y, nc.mDotRadius, paint)
    }

    fun drawRadial(canvas: Canvas, paint: Paint = nc.mHintPaint) {
        canvas.drawLine(nc.mCenterX, nc.mCenterY, dotPoint.x, dotPoint.y, paint)
    }

    fun drawHighlighted(canvas: Canvas) {
        drawRadial(canvas, nc.mHighlightPaint)
        drawDot(canvas, nc.mHighlightPaint)
    }

//        fun draw(canvas: Canvas) {
//            drawRadial(canvas)
//            drawDot(canvas)
//            drawLabel(canvas)
//        }


    fun updatePoints() {
        dotPoint.moveByDistance(nc.mInnerRadius)
        labelPoint.moveByDistance(nc.mLabelRadius)
    }
}


internal fun calculateFifthIndex(note: Note, topNote: Note): Int {
    val semis: Int = note chromaticMinus topNote
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
            "fifthIndex is greater than 11: Note=$note, semis=$semis, topNote=$topNote")
    }
}

internal fun calculateNotePosition(note: Note, order: Order, topNote: Note, temperament: Temperament): Position {
         return when (order) {
            Order.FIFTHS -> {
                val i = calculateFifthIndex(note, topNote)
                i.toDouble() / Order.NUM_FIFTHS
            }
            Order.PITCH -> {
                val ratio: Double = calculatePitchInTemperament(note, temperament) /
                        calculatePitchInTemperament(topNote, temperament)
                Math.log(ratio) / Math.log(2.0)
            }
        }
}

internal fun calculatePitchInTemperament(note: Note, temperament: Temperament) : Hertz {
    return temperament.pitchOf(note) ?: note.defaultPitchIn(temperament)
}




//    val defaultNotes: MutableSet<Note> = (setOf(
//            Note(Note.Letter.A, Note.Accidental.FLAT),
//            Note(Note.Letter.E, Note.Accidental.FLAT),
//            Note(Note.Letter.B, Note.Accidental.FLAT),
//            Note(Note.Letter.F),
//            Note(Note.Letter.C),
//            Note(Note.Letter.G),
//            Note(Note.Letter.D),
//            Note(Note.Letter.A),
//            Note(Note.Letter.E),
//            Note(Note.Letter.B),
//            Note(Note.Letter.F, Note.Accidental.SHARP),
//            Note(Note.Letter.C, Note.Accidental.SHARP)
//        ) - DEFAULT_REFERENCE_NOTE).toMutableSet()

