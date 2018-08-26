package com.thefourthspecies.ttunertouch.notecircle

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG
import com.thefourthspecies.ttunertouch.util.RingList
import kotlin.properties.Delegates

internal data class NCTemperament(var relationships: Set<NCRelationship> = emptySet(),
                         var notes: RingList<NCNote> = RingList<NCNote>()
) {
    companion object {
        fun createFrom(temperament: Temperament, view: ): NCTemperament {
            // TODO
            Log.d(DEBUG_TAG, "NCTemperament.createFrom: not implemented")
            return NCTemperament()
        }
    }
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

internal class NCNote(position: Double, val note: Note, var isHint: Boolean = false, val nc: NoteCircleView) : Comparable<NCNote> {
    // TODO: does this really need to know about Note?
    val position: Position = normalizePosition(position)
    val name: String = note.name
    val dotPoint: Point = Point.polar(position, nc.mInnerRadius)
    val labelPoint: Point = Point.polar(position, nc.mLabelRadius)
    var button: NoteCircleView.NoteButton by Delegates.notNull<NoteCircleView.NoteButton>()

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

    fun updatePoints() {
        dotPoint.moveByDistance(nc.mInnerRadius)
        labelPoint.moveByDistance(nc.mLabelRadius)
    }
}
override val uiRelationships: Set<NoteCirclePresenter.UIRelationship>
        get() = temperament.relationships.map { it.toUI() }.toSet()
    override val uiNotes: Set<NoteCirclePresenter.UINote>
        get() {
            val allNotes = temperament.notes + defaultNotes
            return allNotes.map { it.toUI() }.toSet()
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


    fun Note.toUI(): NoteCirclePresenter.UINote {
        val isHint = defaultNotes.contains(this)
        if (isHint) {
            com.thefourthspecies.ttunertouch.util.assert(!temperament.notes.contains(this)) {
                "Note exists in default form as well as in Temperament: $this"
            }
        }
        return noteCircle.UINote(position, this, isHint)
    }

    fun Relationship.toUI(): NoteCirclePresenter.UIRelationship {
        val isArc = temper.comma != Comma.PURE
        return noteCircle.UIRelationship(fromNote.toUI(), toNote.toUI(), temper.label, isArc)
    }
