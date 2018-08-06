package com.thefourthspecies.ttunertouch.addedittemperament

import com.thefourthspecies.ttunertouch.model.Note

/**
 * Created by Graham on 2018-01-23.
 */
class NoteCircleViewState {
//    // Persistent
//    var notes: Ring<Note>
//    var notePoints: NoteMap
//    var noteButtons: Ring<NoteCircle.NoteButton>
//    var relationships: Set<Relationship>
//
//    // On Draw only
//    var touchDirection: NoteCircle.Direction
//    var startButton: NoteCircle.NoteButton
//    var exceed360: Boolean
//    var sweepAngle: Float = 0f
//
//
//    fun drawScrollInput(canvas: Canvas, startButton: NoteButton?, touchPoint: Point?, sweepAngle: Float) {
//
//        if (startButton != null && touchPoint != null) {
//            val isOuterTouch = touchPoint.distance > mInnerRadius
//            var endNote: Note = startButton.note
//
//            if (isOuterTouch) {
//                val start = startButton.note.point
//                val useCenter = true
//                canvas.drawArc(mInnerCircleBounds, start.screenAngle, sweepAngle, useCenter, mSectorPaint)
//
//                val sweptNotes: List<Note> = mViewState.sweptNotes(mStartButton.note, mSweepAngle)
//                assert(sweptNotes.isNotEmpty()) {
//                    "Touch point exists but no notes swept. startButton=$startButton, touchPoint=$touchPoint"
//                }
//
//                for (note in sweptNotes) {
//                    note.drawDot(canvas)
//                    canvas.drawLine(mCenterX, mCenterY, note.point.x, note.point.y, mSelectPaint)
//                }
//                endNote = sweptNotes.last().note
//            } else {
//                val start = startButton.note.point
//                val touchButton = mNoteButtons.find { it.inSector(touchPoint)  }
//                val end = if (touchButton == null ||
//                        startButton == touchButton ||
//                        touchPoint.distance < mButtonRadius) {
//                    touchPoint
//                } else {
//                    endNote = touchButton.note
//                    touchButton.note.point
//                }
//                canvas.drawLine(start.x, start.y, end.x, end.y, mSelectPaint)
//            }
//
//            startButton.note.drawDot(canvas, mSelectPaint)
//            endNote.drawDot(canvas, mSelectPaint)
//        }
//    }
}

class NoteMap(val map: HashMap<Note, Point>) : MutableMap<Note, Point> by map {
    fun at(note: Note): Point {
        val point = map[note]
        com.thefourthspecies.ttunertouch.util.assert(point != null) {
            "Note $note has no corresponding Point."
        }
        return point ?: Point.polar(0.0, 0f)
    }
}

class Ring() {}

data class Selection(val startNote: Note, val endNote: Note, val direction: Direction)

enum class Direction {
    ASCENDING,
    DESCENDING
}



